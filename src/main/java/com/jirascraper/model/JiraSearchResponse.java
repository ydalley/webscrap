package com.jirascraper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Model representing the response from Jira search API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraSearchResponse {

    @JsonProperty("startAt")
    private int startAt;

    @JsonProperty("maxResults")
    private int maxResults;

    @JsonProperty("total")
    private int total;

    @JsonProperty("issues")
    private List<JiraIssue> issues;

    public int getStartAt() {
        return startAt;
    }

    public void setStartAt(int startAt) {
        this.startAt = startAt;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<JiraIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<JiraIssue> issues) {
        this.issues = issues;
    }
}
