package com.civicconnect.dto;

public class FeedbackRequest {

    private boolean resolved;
    private String comments;

    public FeedbackRequest() {}

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
