package com.civicconnect.dto;

import java.util.List;
import java.util.Map;

public class ChatRequest {

    private String message;
    private String language;
    private List<Map<String, String>> history;

    public ChatRequest() {}

    public ChatRequest(String message, String language, List<Map<String, String>> history) {
        this.message = message;
        this.language = language;
        this.history = history;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<Map<String, String>> getHistory() {
        return history;
    }

    public void setHistory(List<Map<String, String>> history) {
        this.history = history;
    }
}
