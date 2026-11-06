package com.civicconnect.dto;

import java.util.List;

public class ChatResponse {

    private String reply;
    private List<String> suggestions;
    private String language;
    private boolean aiPowered;

    public ChatResponse() {}

    public ChatResponse(String reply, List<String> suggestions, String language, boolean aiPowered) {
        this.reply = reply;
        this.suggestions = suggestions;
        this.language = language;
        this.aiPowered = aiPowered;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isAiPowered() {
        return aiPowered;
    }

    public void setAiPowered(boolean aiPowered) {
        this.aiPowered = aiPowered;
    }
}
