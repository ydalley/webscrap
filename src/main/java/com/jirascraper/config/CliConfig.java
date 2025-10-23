package com.jirascraper.config;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command line configuration for Jira scraper using Picocli.
 */
@Command(
    name = "jira-scraper",
    description = "Scrape Jira issues and transform to LLM training data",
    version = "1.0.0",
    mixinStandardHelpOptions = true
)
public class CliConfig {

    @Option(
        names = {"-p", "--projects"},
        description = "Comma-separated list of Jira project keys to scrape (e.g., KAFKA,SPARK). Default: KAFKA,SPARK,HADOOP",
        split = ","
    )
    private String[] projects;

    @Option(
        names = {"-s", "--page-size"},
        description = "Number of issues to fetch per API request (default: 50)"
    )
    private Integer pageSize;

    @Option(
        names = {"-r", "--rate-limit"},
        description = "Maximum requests per second (default: 5)"
    )
    private Integer rateLimit;

    @Option(
        names = {"-o", "--output-dir"},
        description = "Directory for output JSONL files (default: output)"
    )
    private String outputDir;

    @Option(
        names = {"-c", "--checkpoint-dir"},
        description = "Directory for checkpoint files (default: checkpoints)"
    )
    private String checkpointDir;

    @Option(
        names = {"-i", "--checkpoint-interval"},
        description = "Save checkpoint every N issues (default: 10)"
    )
    private Integer checkpointInterval;

    @Option(
        names = {"-m", "--max-retries"},
        description = "Maximum number of retry attempts on failure (default: 3)"
    )
    private Integer maxRetries;

    @Option(
        names = {"--jira-url"},
        description = "Jira base URL (default: https://issues.apache.org/jira)"
    )
    private String jiraUrl;

    @Option(
        names = {"--connect-timeout"},
        description = "HTTP connect timeout in seconds (default: 30)"
    )
    private Integer connectTimeout;

    @Option(
        names = {"--read-timeout"},
        description = "HTTP read timeout in seconds (default: 60)"
    )
    private Integer readTimeout;

    @Option(
        names = {"--max-file-size"},
        description = "Maximum output file size in MB before creating a new chunk (default: 50)"
    )
    private Long maxFileSizeMB;

    @Option(
        names = {"-sd", "--start-date"},
        description = "Start date for filtering issues by created date (format: YYYY-MM-DD, e.g., 2024-01-01)"
    )
    private String startDate;

    @Option(
        names = {"-ed", "--end-date"},
        description = "End date for filtering issues by created date (format: YYYY-MM-DD, e.g., 2024-12-31)"
    )
    private String endDate;

    // Getters
    public String[] getProjects() {
        return projects;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getRateLimit() {
        return rateLimit;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getCheckpointDir() {
        return checkpointDir;
    }

    public Integer getCheckpointInterval() {
        return checkpointInterval;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public String getJiraUrl() {
        return jiraUrl;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public Long getMaxFileSizeMB() {
        return maxFileSizeMB;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    // Setters (for testing)
    public void setProjects(String[] projects) {
        this.projects = projects;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public void setRateLimit(Integer rateLimit) {
        this.rateLimit = rateLimit;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public void setCheckpointDir(String checkpointDir) {
        this.checkpointDir = checkpointDir;
    }

    public void setCheckpointInterval(Integer checkpointInterval) {
        this.checkpointInterval = checkpointInterval;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setJiraUrl(String jiraUrl) {
        this.jiraUrl = jiraUrl;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setMaxFileSizeMB(Long maxFileSizeMB) {
        this.maxFileSizeMB = maxFileSizeMB;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}
