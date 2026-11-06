package com.civicconnect.dto;

import com.civicconnect.model.Complaint;

import java.util.List;

public class DuplicateCandidate {

    private final Complaint complaint;
    private final double similarityScore;
    private final List<String> matchReasons;

    public DuplicateCandidate(Complaint complaint, double similarityScore, List<String> matchReasons) {
        this.complaint = complaint;
        this.similarityScore = similarityScore;
        this.matchReasons = matchReasons;
    }

    public Complaint getComplaint() {
        return complaint;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }
}
