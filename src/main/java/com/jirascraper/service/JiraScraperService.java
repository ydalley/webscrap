package com.jirascraper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jirascraper.client.JiraApiClient;
import com.jirascraper.config.ScraperConfig;
import com.jirascraper.model.Checkpoint;
import com.jirascraper.model.JiraIssue;
import com.jirascraper.model.JiraSearchResponse;
import com.jirascraper.model.LLMTrainingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main scraper service that orchestrates the scraping process with pagination,
 * checkpointing, and error handling.
 */
public class JiraScraperService {

    private static final Logger logger = LoggerFactory.getLogger(JiraScraperService.class);

    private final JiraApiClient apiClient;
    private final CheckpointService checkpointService;
    private final DataTransformationService transformationService;
    private final ObjectMapper objectMapper;
    private final Path outputDir;
    private final ScraperConfig config;
    private final long maxFileSizeBytes;

    public JiraScraperService(ScraperConfig config) {
        this.config = config;
        this.apiClient = new JiraApiClient(config);
        this.checkpointService = new CheckpointService(config);
        this.transformationService = new DataTransformationService();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.INDENT_OUTPUT); // Compact JSON for JSONL

        this.outputDir = Paths.get(config.getOutputDir());
        this.maxFileSizeBytes = config.getMaxOutputFileSizeMB() * 1024 * 1024; // Convert MB to bytes

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            logger.error("Failed to create output directory", e);
        }
    }

    /**
     * Scrape issues from a specific project with automatic file chunking.
     */
    public void scrapeProject(String projectKey) {
        logger.info("Starting scraping for project: {}", projectKey);

        // Log date filters if present
        if (config.getStartDate() != null || config.getEndDate() != null) {
            if (config.getStartDate() != null && config.getEndDate() != null) {
                logger.info("Date filter: created >= {} AND created <= {}", config.getStartDate(), config.getEndDate());
            } else if (config.getStartDate() != null) {
                logger.info("Date filter: created >= {}", config.getStartDate());
            } else {
                logger.info("Date filter: created <= {}", config.getEndDate());
            }
        }

        // Check for existing checkpoint
        Checkpoint checkpoint = checkpointService.loadCheckpoint(projectKey);
        int startAt = checkpoint != null ? checkpoint.getStartAt() : 0;
        int totalProcessed = checkpoint != null ? checkpoint.getTotalProcessed() : 0;
        int currentChunk = checkpoint != null ? checkpoint.getCurrentChunk() : 1;
        String baseTimestamp = checkpoint != null && checkpoint.getBaseTimestamp() != null
                ? checkpoint.getBaseTimestamp()
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        // Smart checkpoint handling with date filters
        if (checkpoint != null) {
            logger.info("Resuming from checkpoint: startAt={}, totalProcessed={}, chunk={}",
                    startAt, totalProcessed, currentChunk);
        }

        // Create initial output file
        File outputFile = getOutputFile(projectKey, baseTimestamp, currentChunk);
        boolean appendMode = checkpoint != null;

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outputFile, appendMode));

            boolean hasMore = true;
            int pageSize = config.getDefaultPageSize();

            while (hasMore) {
                try {
                    logger.info("Fetching issues for project {} starting at {} (chunk {})", projectKey, startAt, currentChunk);

                    // Fetch a page of issues with date filters
                    JiraSearchResponse response = apiClient.searchIssues(
                            projectKey,
                            startAt,
                            pageSize,
                            config.getStartDate(),
                            config.getEndDate()
                    );

                    if (response == null || response.getIssues() == null) {
                        logger.warn("Received null or empty response for project {}", projectKey);
                        break;
                    }

                    List<JiraIssue> issues = response.getIssues();
                    logger.info("Fetched {} issues (total: {})", issues.size(), response.getTotal());

                    // Process each issue
                    for (JiraIssue issue : issues) {
                        try {
                            // Check if we need to rotate to a new chunk file
                            if (outputFile.length() >= maxFileSizeBytes) {
                                logger.info("File size limit reached ({}MB), rotating to chunk {}",
                                        outputFile.length() / (1024 * 1024), currentChunk + 1);

                                // Close current writer
                                writer.close();

                                // Increment chunk number
                                currentChunk++;

                                // Create new output file
                                outputFile = getOutputFile(projectKey, baseTimestamp, currentChunk);
                                writer = new BufferedWriter(new FileWriter(outputFile, false)); // New file, no append
                                appendMode = false;
                            }

                            processIssue(issue, writer);
                            totalProcessed++;

                            // Save checkpoint periodically
                            if (totalProcessed % config.getCheckpointInterval() == 0) {
                                saveCheckpoint(projectKey, issue.getKey(), startAt + issues.size(), totalProcessed, currentChunk, baseTimestamp);
                            }

                        } catch (Exception e) {
                            logger.error("Failed to process issue {}: {}", issue.getKey(), e.getMessage(), e);
                            // Continue processing other issues despite individual failures
                        }
                    }

                    // Check if there are more pages
                    startAt += issues.size();
                    hasMore = startAt < response.getTotal() && !issues.isEmpty();

                    if (!hasMore) {
                        logger.info("Completed scraping project {}: {} total issues processed across {} chunks",
                                projectKey, totalProcessed, currentChunk);
                        // Mark checkpoint as completed and delete it
                        checkpointService.deleteCheckpoint(projectKey);
                    } else {
                        // Save checkpoint after each page
                        String lastKey = issues.isEmpty() ? null : issues.get(issues.size() - 1).getKey();
                        saveCheckpoint(projectKey, lastKey, startAt, totalProcessed, currentChunk, baseTimestamp);
                    }

                } catch (IOException e) {
                    logger.error("Error fetching issues for project {}: {}", projectKey, e.getMessage(), e);

                    if (isRateLimitError(e) || isServerError(e)) {
                        logger.info("Saving checkpoint and will retry later");
                        saveCheckpoint(projectKey, null, startAt, totalProcessed, currentChunk, baseTimestamp);
                        throw new RuntimeException("Scraping paused due to API error. Resume later.", e);
                    } else {
                        // For other errors, log and continue
                        logger.warn("Skipping this batch due to error, will continue with next");
                        startAt += pageSize;
                    }
                }
            }

        } catch (IOException e) {
            logger.error("Failed to write to output file for project {}", projectKey, e);
            saveCheckpoint(projectKey, null, startAt, totalProcessed, currentChunk, baseTimestamp);
        } catch (Exception e) {
            logger.error("Unexpected error while scraping project {}", projectKey, e);
            saveCheckpoint(projectKey, null, startAt, totalProcessed, currentChunk, baseTimestamp);
        } finally {
            // Ensure writer is closed
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.error("Failed to close writer for project {}", projectKey, e);
                }
            }
        }
    }


    /**
     * Process a single issue: transform and write to output.
     */
    private void processIssue(JiraIssue issue, BufferedWriter writer) throws IOException {
        logger.debug("Processing issue: {}", issue.getKey());

        // Transform to LLM training data
        LLMTrainingData trainingData = transformationService.transform(issue);

        if (trainingData == null) {
            logger.warn("Failed to transform issue {}, skipping", issue.getKey());
            return;
        }

        // Write as JSONL (one JSON object per line)
        String json = objectMapper.writeValueAsString(trainingData);
        writer.write(json);
        writer.newLine();
        writer.flush();

        logger.debug("Successfully processed issue: {}", issue.getKey());
    }

    /**
     * Save a checkpoint with chunk information.
     */
    private void saveCheckpoint(String projectKey, String lastIssueKey, int startAt, int totalProcessed, int currentChunk, String baseTimestamp) {
        Checkpoint checkpoint = new Checkpoint(projectKey, lastIssueKey, startAt, totalProcessed, currentChunk, baseTimestamp);
        checkpointService.saveCheckpoint(checkpoint);
    }

    /**
     * Get the output file for a project with chunk number.
     */
    private File getOutputFile(String projectKey, String baseTimestamp, int chunkNumber) {
        String filename;
        if (chunkNumber == 1) {
            // First chunk doesn't have chunk suffix for backward compatibility
            filename = String.format("%s_%s.jsonl", projectKey, baseTimestamp);
        } else {
            // Subsequent chunks have chunk number
            filename = String.format("%s_%s_chunk_%03d.jsonl", projectKey, baseTimestamp, chunkNumber);
        }
        return outputDir.resolve(filename).toFile();
    }

    /**
     * Check if an exception is a rate limit error.
     */
    private boolean isRateLimitError(Exception e) {
        return e.getMessage() != null && e.getMessage().contains("429");
    }

    /**
     * Check if an exception is a server error (5xx).
     */
    private boolean isServerError(Exception e) {
        if (e.getMessage() == null) {
            return false;
        }
        String message = e.getMessage();
        return message.contains("500") || message.contains("502") ||
               message.contains("503") || message.contains("504");
    }

    /**
     * Close resources.
     */
    public void close() {
        apiClient.close();
    }
}
