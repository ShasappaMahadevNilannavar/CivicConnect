package com.civicconnect.service;

import com.civicconnect.dto.DuplicateCandidate;
import com.civicconnect.model.Complaint;
import com.civicconnect.model.enums.ComplaintStatus;
import com.civicconnect.repository.ComplaintRepository;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DuplicateDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateDetectionService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final ComplaintRepository complaintRepository;
    private final JaroWinklerSimilarity textSimilarity = new JaroWinklerSimilarity();

    @Value("${app.duplicate.similarity-threshold:0.6}")
    private double similarityThreshold;

    @Value("${app.duplicate.proximity-radius-km:0.5}")
    private double proximityRadiusKm;

    @Value("${app.duplicate.max-results:5}")
    private int maxResults;

    public DuplicateDetectionService(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    public List<DuplicateCandidate> findPotentialDuplicates(Complaint complaint) {
        if (complaint == null || complaint.getCategory() == null) {
            return List.of();
        }

        Long excludeId = complaint.getId() != null ? complaint.getId() : -1L;
        List<Complaint> candidates = complaintRepository.findByCategoryExcluding(complaint.getCategory(), excludeId);

        List<DuplicateCandidate> potentialDuplicates = new ArrayList<>();

        for (Complaint candidate : candidates) {
            if (candidate.getStatus() == ComplaintStatus.REJECTED) {
                continue;
            }

            List<String> matchReasons = new ArrayList<>();
            matchReasons.add("Same Category: " + complaint.getCategory().getName());

            // 1. Title similarity
            double titleScore = 0.0;
            if (complaint.getTitle() != null && candidate.getTitle() != null) {
                titleScore = textSimilarity.apply(complaint.getTitle().toLowerCase().trim(),
                        candidate.getTitle().toLowerCase().trim());
                if (titleScore >= 0.7) {
                    matchReasons.add(String.format("High title similarity (%.0f%%)", titleScore * 100));
                }
            }

            // 2. Description similarity
            double descScore = 0.0;
            if (complaint.getDescription() != null && candidate.getDescription() != null) {
                descScore = textSimilarity.apply(complaint.getDescription().toLowerCase().trim(),
                        candidate.getDescription().toLowerCase().trim());
                if (descScore >= 0.65) {
                    matchReasons.add(String.format("High description similarity (%.0f%%)", descScore * 100));
                }
            }

            // 3. Geographic proximity
            double geoScore = 0.0;
            boolean hasCoordinates = complaint.getLatitude() != null && complaint.getLongitude() != null &&
                                    candidate.getLatitude() != null && candidate.getLongitude() != null;
            if (hasCoordinates) {
                double distanceKm = haversineDistance(
                        complaint.getLatitude(), complaint.getLongitude(),
                        candidate.getLatitude(), candidate.getLongitude());
                if (distanceKm <= proximityRadiusKm) {
                    geoScore = 1.0;
                    matchReasons.add(String.format("Geographic proximity: within %.2f km (actual: %.2f km)", proximityRadiusKm, distanceKm));
                } else if (distanceKm <= proximityRadiusKm * 2) {
                    geoScore = 0.5;
                    matchReasons.add(String.format("Nearby location (%.2f km away)", distanceKm));
                }
            }

            // Total weighted similarity score
            double totalScore;
            if (hasCoordinates) {
                totalScore = (titleScore * 0.35) + (descScore * 0.35) + (geoScore * 0.30);
            } else {
                totalScore = (titleScore * 0.45) + (descScore * 0.55);
            }

            if (totalScore >= similarityThreshold) {
                potentialDuplicates.add(new DuplicateCandidate(candidate, totalScore, matchReasons));
            }
        }

        return potentialDuplicates.stream()
                .sorted(Comparator.comparingDouble(DuplicateCandidate::getSimilarityScore).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    public double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
