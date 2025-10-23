package com.jirascraper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jirascraper.config.ScraperConfig;
import com.jirascraper.model.Checkpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for managing checkpoint persistence to enable resumable scraping.
 */
public class CheckpointService {

    private static final Logger logger = LoggerFactory.getLogger(CheckpointService.class);

    private final ObjectMapper objectMapper;
    private final Path checkpointDir;

    public CheckpointService(ScraperConfig config) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        this.checkpointDir = Paths.get(config.getCheckpointDir());

        try {
            Files.createDirectories(checkpointDir);
        } catch (IOException e) {
            logger.error("Failed to create checkpoint directory", e);
        }
    }

    /**
     * Save a checkpoint for a project.
     */
    public void saveCheckpoint(Checkpoint checkpoint) {
        try {
            File checkpointFile = getCheckpointFile(checkpoint.getProjectKey());
            objectMapper.writeValue(checkpointFile, checkpoint);
            logger.info("Checkpoint saved for project {}: {} issues processed",
                    checkpoint.getProjectKey(), checkpoint.getTotalProcessed());
        } catch (IOException e) {
            logger.error("Failed to save checkpoint for project {}", checkpoint.getProjectKey(), e);
        }
    }

    /**
     * Load a checkpoint for a project if it exists.
     */
    public Checkpoint loadCheckpoint(String projectKey) {
        try {
            File checkpointFile = getCheckpointFile(projectKey);

            if (!checkpointFile.exists()) {
                logger.info("No checkpoint found for project {}", projectKey);
                return null;
            }

            Checkpoint checkpoint = objectMapper.readValue(checkpointFile, Checkpoint.class);
            logger.info("Checkpoint loaded for project {}: resuming from {} issues processed",
                    projectKey, checkpoint.getTotalProcessed());
            return checkpoint;

        } catch (IOException e) {
            logger.error("Failed to load checkpoint for project {}", projectKey, e);
            return null;
        }
    }

    /**
     * Delete a checkpoint for a project (typically when scraping is complete).
     */
    public void deleteCheckpoint(String projectKey) {
        try {
            File checkpointFile = getCheckpointFile(projectKey);
            if (checkpointFile.exists()) {
                Files.delete(checkpointFile.toPath());
                logger.info("Checkpoint deleted for project {}", projectKey);
            }
        } catch (IOException e) {
            logger.error("Failed to delete checkpoint for project {}", projectKey, e);
        }
    }

    /**
     * Check if a checkpoint exists for a project.
     */
    public boolean hasCheckpoint(String projectKey) {
        return getCheckpointFile(projectKey).exists();
    }

    /**
     * Get the checkpoint file for a project.
     */
    private File getCheckpointFile(String projectKey) {
        return checkpointDir.resolve(projectKey + "_checkpoint.json").toFile();
    }
}
