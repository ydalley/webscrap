package com.jirascraper.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for Jira scraper settings.
 * Supports configuration priority: CLI args > Properties file > Hardcoded defaults
 */
public class ScraperConfig {

    // Default values (lowest priority)
    private static final String DEFAULT_JIRA_BASE_URL = "https://issues.apache.org/jira";
    private static final int DEFAULT_MAX_REQUESTS_PER_SECOND = 5;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_INITIAL_BACKOFF_MS = 1000;
    private static final int DEFAULT_MAX_BACKOFF_MS = 30000;
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_READ_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_WRITE_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int DEFAULT_MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_CHECKPOINT_DIR = "checkpoints";
    private static final int DEFAULT_CHECKPOINT_INTERVAL = 10;
    private static final String DEFAULT_OUTPUT_DIR = "output";
    private static final String DEFAULT_OUTPUT_FORMAT = "jsonl";
    private static final long DEFAULT_MAX_OUTPUT_FILE_SIZE_MB = 50; // 50 MB default chunk size

    // Instance fields (can be overridden via CLI or properties)
    private final String jiraBaseUrl;
    private final List<String> projects;
    private final int maxRequestsPerSecond;
    private final int requestDelayMs;
    private final int maxRetries;
    private final int initialBackoffMs;
    private final int maxBackoffMs;
    private final double backoffMultiplier;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final int writeTimeoutSeconds;
    private final int defaultPageSize;
    private final int maxPageSize;
    private final String checkpointDir;
    private final int checkpointInterval;
    private final String outputDir;
    private final String outputFormat;
    private final long maxOutputFileSizeMB;
    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * Constructor with default values.
     */
    public ScraperConfig() {
        this(null);
    }

    /**
     * Constructor that merges CLI config with defaults.
     * Priority: CLI args > Properties file > Hardcoded defaults
     */
    public ScraperConfig(CliConfig cliConfig) {
        // Jira URL
        this.jiraBaseUrl = cliConfig != null && cliConfig.getJiraUrl() != null
                ? cliConfig.getJiraUrl()
                : DEFAULT_JIRA_BASE_URL;

        // Projects (must be provided via CLI)
        this.projects = cliConfig != null && cliConfig.getProjects() != null && cliConfig.getProjects().length > 0
                ? Arrays.asList(cliConfig.getProjects())
                : null;

        // Page size
        this.defaultPageSize = cliConfig != null && cliConfig.getPageSize() != null
                ? cliConfig.getPageSize()
                : DEFAULT_PAGE_SIZE;

        this.maxPageSize = DEFAULT_MAX_PAGE_SIZE;

        // Rate limiting
        this.maxRequestsPerSecond = cliConfig != null && cliConfig.getRateLimit() != null
                ? cliConfig.getRateLimit()
                : DEFAULT_MAX_REQUESTS_PER_SECOND;

        this.requestDelayMs = 1000 / this.maxRequestsPerSecond;

        // Retry configuration
        this.maxRetries = cliConfig != null && cliConfig.getMaxRetries() != null
                ? cliConfig.getMaxRetries()
                : DEFAULT_MAX_RETRIES;

        this.initialBackoffMs = DEFAULT_INITIAL_BACKOFF_MS;
        this.maxBackoffMs = DEFAULT_MAX_BACKOFF_MS;
        this.backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;

        // Timeout configuration
        this.connectTimeoutSeconds = cliConfig != null && cliConfig.getConnectTimeout() != null
                ? cliConfig.getConnectTimeout()
                : DEFAULT_CONNECT_TIMEOUT_SECONDS;

        this.readTimeoutSeconds = cliConfig != null && cliConfig.getReadTimeout() != null
                ? cliConfig.getReadTimeout()
                : DEFAULT_READ_TIMEOUT_SECONDS;

        this.writeTimeoutSeconds = DEFAULT_WRITE_TIMEOUT_SECONDS;

        // Checkpoint configuration
        this.checkpointDir = cliConfig != null && cliConfig.getCheckpointDir() != null
                ? cliConfig.getCheckpointDir()
                : DEFAULT_CHECKPOINT_DIR;

        this.checkpointInterval = cliConfig != null && cliConfig.getCheckpointInterval() != null
                ? cliConfig.getCheckpointInterval()
                : DEFAULT_CHECKPOINT_INTERVAL;

        // Output configuration
        this.outputDir = cliConfig != null && cliConfig.getOutputDir() != null
                ? cliConfig.getOutputDir()
                : DEFAULT_OUTPUT_DIR;

        this.outputFormat = DEFAULT_OUTPUT_FORMAT;

        this.maxOutputFileSizeMB = cliConfig != null && cliConfig.getMaxFileSizeMB() != null
                ? cliConfig.getMaxFileSizeMB()
                : DEFAULT_MAX_OUTPUT_FILE_SIZE_MB;

        // Date filters (optional)
        this.startDate = parseDate(cliConfig != null ? cliConfig.getStartDate() : null, "start-date");
        this.endDate = parseDate(cliConfig != null ? cliConfig.getEndDate() : null, "end-date");

        // Validate date logic
        validateDates();
    }

    /**
     * Parse date string in YYYY-MM-DD format.
     */
    private LocalDate parseDate(String dateString, String fieldName) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateString.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                String.format("Invalid %s format: '%s'. Expected format: YYYY-MM-DD (e.g., 2024-01-15)",
                    fieldName, dateString), e);
        }
    }

    /**
     * Validate date logic.
     */
    private void validateDates() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                String.format("Start date (%s) cannot be after end date (%s)",
                    startDate, endDate));
        }
    }

    // Getters
    public String getJiraBaseUrl() {
        return jiraBaseUrl;
    }

    public List<String> getDefaultProjects() {
        return projects;
    }

    public int getMaxRequestsPerSecond() {
        return maxRequestsPerSecond;
    }

    public int getRequestDelayMs() {
        return requestDelayMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getInitialBackoffMs() {
        return initialBackoffMs;
    }

    public int getMaxBackoffMs() {
        return maxBackoffMs;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public int getWriteTimeoutSeconds() {
        return writeTimeoutSeconds;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public String getCheckpointDir() {
        return checkpointDir;
    }

    public int getCheckpointInterval() {
        return checkpointInterval;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public long getMaxOutputFileSizeMB() {
        return maxOutputFileSizeMB;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    @Override
    public String toString() {
        return "ScraperConfig{" +
                "jiraBaseUrl='" + jiraBaseUrl + '\'' +
                ", projects=" + projects +
                ", maxRequestsPerSecond=" + maxRequestsPerSecond +
                ", maxRetries=" + maxRetries +
                ", defaultPageSize=" + defaultPageSize +
                ", checkpointDir='" + checkpointDir + '\'' +
                ", checkpointInterval=" + checkpointInterval +
                ", outputDir='" + outputDir + '\'' +
                ", maxOutputFileSizeMB=" + maxOutputFileSizeMB +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}
