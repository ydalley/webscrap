package com.jirascraper.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jirascraper.config.ScraperConfig;
import com.jirascraper.model.JiraIssue;
import com.jirascraper.model.JiraSearchResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Client for interacting with Jira REST API with rate limiting and retry logic.
 */
public class JiraApiClient {

    private static final Logger logger = LoggerFactory.getLogger(JiraApiClient.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final ScraperConfig config;
    private long lastRequestTime = 0;

    public JiraApiClient(ScraperConfig config) {
        this.config = config;
        this.baseUrl = config.getJiraBaseUrl();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(config.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(config.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
                .addInterceptor(new RateLimitInterceptor())
                .addInterceptor(new RetryInterceptor())
                .build();
    }

    /**
     * Search for issues in a specific project with pagination.
     */
    public JiraSearchResponse searchIssues(String projectKey, int startAt, int maxResults) throws IOException {
        return searchIssues(projectKey, startAt, maxResults, null, null);
    }

    /**
     * Search for issues in a specific project with pagination and optional date filters.
     */
    public JiraSearchResponse searchIssues(String projectKey, int startAt, int maxResults,
                                          LocalDate startDate, LocalDate endDate) throws IOException {
        String jql = buildJqlQuery(projectKey, startDate, endDate);
        String url = String.format("%s/rest/api/2/search?jql=%s&startAt=%d&maxResults=%d&fields=*all",
                baseUrl,
                jql.replace(" ", "+"),
                startAt,
                maxResults);

        logger.debug("Fetching issues from: {}", url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return executeRequest(request, JiraSearchResponse.class);
    }

    /**
     * Build JQL query with optional date filters.
     */
    private String buildJqlQuery(String projectKey, LocalDate startDate, LocalDate endDate) {
        StringBuilder jql = new StringBuilder();
        jql.append("project = ").append(projectKey);

        if (startDate != null) {
            jql.append(" AND created >= \"").append(startDate).append("\"");
        }

        if (endDate != null) {
            jql.append(" AND created <= \"").append(endDate).append("\"");
        }

        jql.append(" ORDER BY created ASC");

        logger.debug("Built JQL query: {}", jql);
        return jql.toString();
    }

    /**
     * Get a single issue by key with all fields and comments.
     */
    public JiraIssue getIssue(String issueKey) throws IOException {
        String url = String.format("%s/rest/api/2/issue/%s?fields=*all&expand=renderedFields,names,schema,transitions,operations,changelog,comment",
                baseUrl,
                issueKey);

        logger.debug("Fetching issue: {}", issueKey);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return executeRequest(request, JiraIssue.class);
    }

    /**
     * Execute an HTTP request with proper error handling.
     */
    private <T> T executeRequest(Request request, Class<T> responseType) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException(String.format("Request failed with status %d: %s",
                        response.code(), errorBody));
            }

            if (response.body() == null) {
                throw new IOException("Empty response body");
            }

            String responseBody = response.body().string();

            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new IOException("Empty or null response body");
            }

            try {
                return objectMapper.readValue(responseBody, responseType);
            } catch (Exception e) {
                logger.error("Failed to parse response: {}", responseBody.substring(0, Math.min(500, responseBody.length())));
                throw new IOException("Failed to parse JSON response: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Rate limiting interceptor to respect API limits.
     */
    private class RateLimitInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            synchronized (JiraApiClient.this) {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastRequest = currentTime - lastRequestTime;
                long delayNeeded = config.getRequestDelayMs() - timeSinceLastRequest;

                if (delayNeeded > 0) {
                    try {
                        logger.debug("Rate limiting: waiting {}ms", delayNeeded);
                        Thread.sleep(delayNeeded);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Rate limiting interrupted", e);
                    }
                }

                lastRequestTime = System.currentTimeMillis();
            }

            return chain.proceed(chain.request());
        }
    }

    /**
     * Retry interceptor with exponential backoff for handling transient failures.
     */
    private class RetryInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException lastException = null;

            int attempt = 0;
            int maxRetries = config.getMaxRetries();

            while (attempt < maxRetries) {
                try {
                    response = chain.proceed(request);

                    // Check if we should retry based on status code
                    if (shouldRetry(response)) {
                        int statusCode = response.code();
                        response.close();

                        if (statusCode == 429) {
                            logger.warn("Rate limit hit (429), attempt {}/{}", attempt + 1, maxRetries);
                        } else {
                            logger.warn("Retryable error {} on attempt {}/{}", statusCode, attempt + 1, maxRetries);
                        }

                        if (attempt < maxRetries - 1) {
                            long backoffTime = calculateBackoff(attempt);
                            logger.debug("Backing off for {}ms", backoffTime);
                            Thread.sleep(backoffTime);
                        }

                        attempt++;
                        continue;
                    }

                    // Success - return response
                    return response;

                } catch (IOException e) {
                    lastException = e;
                    logger.warn("Request failed on attempt {}/{}: {}", attempt + 1, maxRetries, e.getMessage());

                    if (response != null) {
                        response.close();
                    }

                    if (attempt < maxRetries - 1) {
                        try {
                            long backoffTime = calculateBackoff(attempt);
                            logger.debug("Backing off for {}ms", backoffTime);
                            Thread.sleep(backoffTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Retry interrupted", ie);
                        }
                    }

                    attempt++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (response != null) {
                        response.close();
                    }
                    throw new IOException("Retry interrupted", e);
                }
            }

            // All retries exhausted
            if (lastException != null) {
                throw lastException;
            }

            // This shouldn't happen, but just in case
            throw new IOException("Max retries exceeded");
        }

        private boolean shouldRetry(Response response) {
            int code = response.code();
            // Retry on 429 (rate limit), 5xx errors, and 408 (timeout)
            return code == 429 || code == 408 || (code >= 500 && code < 600);
        }

        private long calculateBackoff(int attempt) {
            long backoff = (long) (config.getInitialBackoffMs() *
                    Math.pow(config.getBackoffMultiplier(), attempt));
            return Math.min(backoff, config.getMaxBackoffMs());
        }
    }

    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
