package com.jirascraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Model representing the transformed data for LLM training in JSONL format.
 */
public class LLMTrainingData {

    @JsonProperty("issue_key")
    private String issueKey;

    @JsonProperty("project")
    private String project;

    @JsonProperty("issue_type")
    private String issueType;

    @JsonProperty("title")
    private String title;

    @JsonProperty("status")
    private String status;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("reporter")
    private String reporter;

    @JsonProperty("assignee")
    private String assignee;

    @JsonProperty("created")
    private String created;

    @JsonProperty("updated")
    private String updated;

    @JsonProperty("resolution_date")
    private String resolutionDate;

    @JsonProperty("resolution")
    private String resolution;

    @JsonProperty("labels")
    private List<String> labels;

    @JsonProperty("description")
    private String description;

    @JsonProperty("comments")
    private List<CommentData> comments;

    @JsonProperty("tasks")
    private Tasks tasks;

    public static class CommentData {
        @JsonProperty("author")
        private String author;

        @JsonProperty("body")
        private String body;

        @JsonProperty("created")
        private String created;

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
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

    public static class Tasks {
        @JsonProperty("summarization")
        private String summarization;

        @JsonProperty("classification")
        private Classification classification;

        @JsonProperty("qna")
        private List<QnA> qna;

        public String getSummarization() {
            return summarization;
        }

        public void setSummarization(String summarization) {
            this.summarization = summarization;
        }

        public Classification getClassification() {
            return classification;
        }

        public void setClassification(Classification classification) {
            this.classification = classification;
        }

        public List<QnA> getQna() {
            return qna;
        }

        public void setQna(List<QnA> qna) {
            this.qna = qna;
        }
    }

    public static class Classification {
        @JsonProperty("issue_type")
        private String issueType;

        @JsonProperty("priority")
        private String priority;

        @JsonProperty("status")
        private String status;

        public String getIssueType() {
            return issueType;
        }

        public void setIssueType(String issueType) {
            this.issueType = issueType;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class QnA {
        @JsonProperty("question")
        private String question;

        @JsonProperty("answer")
        private String answer;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }

    // Getters and Setters
    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
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

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<CommentData> getComments() {
        return comments;
    }

    public void setComments(List<CommentData> comments) {
        this.comments = comments;
    }

    public Tasks getTasks() {
        return tasks;
    }

    public void setTasks(Tasks tasks) {
        this.tasks = tasks;
    }
}
