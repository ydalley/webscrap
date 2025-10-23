package com.jirascraper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model representing a Jira issue from the API response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssue {

    @JsonProperty("id")
    private String id;

    @JsonProperty("key")
    private String key;

    @JsonProperty("fields")
    private Fields fields;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Fields getFields() {
        return fields;
    }

    public void setFields(Fields fields) {
        this.fields = fields;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fields {
        @JsonProperty("summary")
        private String summary;

        @JsonProperty("description")
        private String description;

        @JsonProperty("status")
        private Status status;

        @JsonProperty("priority")
        private Priority priority;

        @JsonProperty("project")
        private Project project;

        @JsonProperty("reporter")
        private User reporter;

        @JsonProperty("assignee")
        private User assignee;

        @JsonProperty("created")
        private String created;

        @JsonProperty("updated")
        private String updated;

        @JsonProperty("resolutiondate")
        private String resolutionDate;

        @JsonProperty("labels")
        private List<String> labels;

        @JsonProperty("comment")
        private CommentContainer comment;

        @JsonProperty("issuetype")
        private IssueType issueType;

        @JsonProperty("resolution")
        private Resolution resolution;

        // Getters and Setters
        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public Priority getPriority() {
            return priority;
        }

        public void setPriority(Priority priority) {
            this.priority = priority;
        }

        public Project getProject() {
            return project;
        }

        public void setProject(Project project) {
            this.project = project;
        }

        public User getReporter() {
            return reporter;
        }

        public void setReporter(User reporter) {
            this.reporter = reporter;
        }

        public User getAssignee() {
            return assignee;
        }

        public void setAssignee(User assignee) {
            this.assignee = assignee;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

        public String getUpdated() {
            return updated;
        }

        public void setUpdated(String updated) {
            this.updated = updated;
        }

        public String getResolutionDate() {
            return resolutionDate;
        }

        public void setResolutionDate(String resolutionDate) {
            this.resolutionDate = resolutionDate;
        }

        public List<String> getLabels() {
            return labels;
        }

        public void setLabels(List<String> labels) {
            this.labels = labels;
        }

        public CommentContainer getComment() {
            return comment;
        }

        public void setComment(CommentContainer comment) {
            this.comment = comment;
        }

        public IssueType getIssueType() {
            return issueType;
        }

        public void setIssueType(IssueType issueType) {
            this.issueType = issueType;
        }

        public Resolution getResolution() {
            return resolution;
        }

        public void setResolution(Resolution resolution) {
            this.resolution = resolution;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        @JsonProperty("name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Priority {
        @JsonProperty("name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Project {
        @JsonProperty("key")
        private String key;

        @JsonProperty("name")
        private String name;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        @JsonProperty("name")
        private String name;

        @JsonProperty("displayName")
        private String displayName;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommentContainer {
        @JsonProperty("comments")
        private List<Comment> comments;

        public List<Comment> getComments() {
            return comments;
        }

        public void setComments(List<Comment> comments) {
            this.comments = comments;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Comment {
        @JsonProperty("id")
        private String id;

        @JsonProperty("author")
        private User author;

        @JsonProperty("body")
        private String body;

        @JsonProperty("created")
        private String created;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public User getAuthor() {
            return author;
        }

        public void setAuthor(User author) {
            this.author = author;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueType {
        @JsonProperty("name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resolution {
        @JsonProperty("name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
