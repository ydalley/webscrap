package com.jirascraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Model representing checkpoint data for resumable scraping.
 */
public class Checkpoint {

    @JsonProperty("project_key")
    private String projectKey;

    @JsonProperty("last_issue_key")
    private String lastIssueKey;

    @JsonProperty("start_at")
    private int startAt;

    @JsonProperty("total_processed")
    private int totalProcessed;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("completed")
    private boolean completed;

    @JsonProperty("current_chunk")
    private int currentChunk;

    @JsonProperty("base_timestamp")
    private String baseTimestamp;

    public Checkpoint() {
        this.timestamp = LocalDateTime.now();
        this.completed = false;
        this.currentChunk = 1;
    }

    public Checkpoint(String projectKey, String lastIssueKey, int startAt, int totalProcessed) {
        this.projectKey = projectKey;
        this.lastIssueKey = lastIssueKey;
        this.startAt = startAt;
        this.totalProcessed = totalProcessed;
        this.timestamp = LocalDateTime.now();
        this.completed = false;
        this.currentChunk = 1;
    }

    public Checkpoint(String projectKey, String lastIssueKey, int startAt, int totalProcessed, int currentChunk, String baseTimestamp) {
        this.projectKey = projectKey;
        this.lastIssueKey = lastIssueKey;
        this.startAt = startAt;
        this.totalProcessed = totalProcessed;
        this.timestamp = LocalDateTime.now();
        this.completed = false;
        this.currentChunk = currentChunk;
        this.baseTimestamp = baseTimestamp;
    }

    // Getters and Setters
    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getLastIssueKey() {
        return lastIssueKey;
    }

    public void setLastIssueKey(String lastIssueKey) {
        this.lastIssueKey = lastIssueKey;
    }

    public int getStartAt() {
        return startAt;
    }

    public void setStartAt(int startAt) {
        this.startAt = startAt;
    }

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getCurrentChunk() {
        return currentChunk;
    }

    public void setCurrentChunk(int currentChunk) {
        this.currentChunk = currentChunk;
    }

    public String getBaseTimestamp() {
        return baseTimestamp;
    }

    public void setBaseTimestamp(String baseTimestamp) {
        this.baseTimestamp = baseTimestamp;
    }
}
