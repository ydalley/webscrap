package com.jirascraper.service;

import com.jirascraper.model.JiraIssue;
import com.jirascraper.model.LLMTrainingData;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for transforming Jira issues into LLM training data format.
 */
public class DataTransformationService {

    private static final Logger logger = LoggerFactory.getLogger(DataTransformationService.class);

    /**
     * Transform a Jira issue into LLM training data.
     */
    public LLMTrainingData transform(JiraIssue issue) {
        try {
            LLMTrainingData data = new LLMTrainingData();

            // Basic metadata
            data.setIssueKey(issue.getKey());

            if (issue.getFields() != null) {
                JiraIssue.Fields fields = issue.getFields();

                // Project information
                if (fields.getProject() != null) {
                    data.setProject(fields.getProject().getKey());
                }

                // Issue type
                if (fields.getIssueType() != null) {
                    data.setIssueType(fields.getIssueType().getName());
                }

                // Title
                data.setTitle(fields.getSummary());

                // Status
                if (fields.getStatus() != null) {
                    data.setStatus(fields.getStatus().getName());
                }

                // Priority
                if (fields.getPriority() != null) {
                    data.setPriority(fields.getPriority().getName());
                }

                // Reporter
                if (fields.getReporter() != null) {
                    data.setReporter(fields.getReporter().getDisplayName() != null ?
                            fields.getReporter().getDisplayName() : fields.getReporter().getName());
                }

                // Assignee
                if (fields.getAssignee() != null) {
                    data.setAssignee(fields.getAssignee().getDisplayName() != null ?
                            fields.getAssignee().getDisplayName() : fields.getAssignee().getName());
                }

                // Timestamps
                data.setCreated(fields.getCreated());
                data.setUpdated(fields.getUpdated());
                data.setResolutionDate(fields.getResolutionDate());

                // Resolution
                if (fields.getResolution() != null) {
                    data.setResolution(fields.getResolution().getName());
                }

                // Labels
                data.setLabels(fields.getLabels() != null ? fields.getLabels() : Collections.emptyList());

                // Description (clean HTML)
                data.setDescription(cleanHtml(fields.getDescription()));

                // Comments
                List<LLMTrainingData.CommentData> comments = transformComments(fields);
                data.setComments(comments);

                // Generate derived tasks
                LLMTrainingData.Tasks tasks = generateTasks(data, fields, comments);
                data.setTasks(tasks);
            }

            return data;

        } catch (Exception e) {
            logger.error("Failed to transform issue {}: {}", issue.getKey(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Transform Jira comments to training data format.
     */
    private List<LLMTrainingData.CommentData> transformComments(JiraIssue.Fields fields) {
        if (fields.getComment() == null || fields.getComment().getComments() == null) {
            return Collections.emptyList();
        }

        return fields.getComment().getComments().stream()
                .map(comment -> {
                    LLMTrainingData.CommentData commentData = new LLMTrainingData.CommentData();
                    if (comment.getAuthor() != null) {
                        commentData.setAuthor(comment.getAuthor().getDisplayName() != null ?
                                comment.getAuthor().getDisplayName() : comment.getAuthor().getName());
                    }
                    commentData.setBody(cleanHtml(comment.getBody()));
                    commentData.setCreated(comment.getCreated());
                    return commentData;
                })
                .collect(Collectors.toList());
    }

    /**
     * Generate derived tasks for LLM training.
     */
    private LLMTrainingData.Tasks generateTasks(LLMTrainingData data,
                                                  JiraIssue.Fields fields,
                                                  List<LLMTrainingData.CommentData> comments) {
        LLMTrainingData.Tasks tasks = new LLMTrainingData.Tasks();

        // Summarization task
        tasks.setSummarization(generateSummarization(data, comments));

        // Classification task
        tasks.setClassification(generateClassification(data));

        // Q&A tasks
        tasks.setQna(generateQnA(data, fields, comments));

        return tasks;
    }

    /**
     * Generate a summarization of the issue.
     */
    private String generateSummarization(LLMTrainingData data, List<LLMTrainingData.CommentData> comments) {
        StringBuilder summary = new StringBuilder();
        summary.append("Issue: ").append(data.getTitle()).append("\n");
        summary.append("Status: ").append(data.getStatus()).append("\n");

        if (data.getDescription() != null && !data.getDescription().trim().isEmpty()) {
            String desc = data.getDescription();
            if (desc.length() > 500) {
                desc = desc.substring(0, 497) + "...";
            }
            summary.append("Description: ").append(desc).append("\n");
        }

        if (comments != null && !comments.isEmpty()) {
            summary.append("Comments: ").append(comments.size()).append(" comment(s)");
        }

        return summary.toString();
    }

    /**
     * Generate classification data.
     */
    private LLMTrainingData.Classification generateClassification(LLMTrainingData data) {
        LLMTrainingData.Classification classification = new LLMTrainingData.Classification();
        classification.setIssueType(data.getIssueType());
        classification.setPriority(data.getPriority());
        classification.setStatus(data.getStatus());
        return classification;
    }

    /**
     * Generate Q&A pairs for the issue.
     */
    private List<LLMTrainingData.QnA> generateQnA(LLMTrainingData data,
                                                    JiraIssue.Fields fields,
                                                    List<LLMTrainingData.CommentData> comments) {
        List<LLMTrainingData.QnA> qnaPairs = new ArrayList<>();

        // Q&A about issue status
        qnaPairs.add(createQnA(
                "What is the status of issue " + data.getIssueKey() + "?",
                "The status is: " + data.getStatus()
        ));

        // Q&A about issue priority
        if (data.getPriority() != null) {
            qnaPairs.add(createQnA(
                    "What is the priority of this issue?",
                    "The priority is: " + data.getPriority()
            ));
        }

        // Q&A about issue type
        if (data.getIssueType() != null) {
            qnaPairs.add(createQnA(
                    "What type of issue is " + data.getIssueKey() + "?",
                    "This is a " + data.getIssueType()
            ));
        }

        // Q&A about resolution
        if (data.getResolution() != null) {
            qnaPairs.add(createQnA(
                    "How was this issue resolved?",
                    "Resolution: " + data.getResolution()
            ));
        }

        // Q&A about comments
        if (comments != null && !comments.isEmpty()) {
            qnaPairs.add(createQnA(
                    "How many comments does this issue have?",
                    "This issue has " + comments.size() + " comment(s)"
            ));
        }

        return qnaPairs;
    }

    /**
     * Helper method to create a Q&A pair.
     */
    private LLMTrainingData.QnA createQnA(String question, String answer) {
        LLMTrainingData.QnA qna = new LLMTrainingData.QnA();
        qna.setQuestion(question);
        qna.setAnswer(answer);
        return qna;
    }

    /**
     * Clean HTML from text content.
     */
    private String cleanHtml(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }

        try {
            // Use Jsoup to parse and extract text from HTML
            String text = Jsoup.parse(html).text();
            // Remove extra whitespace
            return text.replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            logger.warn("Failed to clean HTML, returning as-is: {}", e.getMessage());
            return html;
        }
    }
}
