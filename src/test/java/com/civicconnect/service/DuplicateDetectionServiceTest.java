package com.civicconnect.service;

import com.civicconnect.dto.DuplicateCandidate;
import com.civicconnect.model.Category;
import com.civicconnect.model.Complaint;
import com.civicconnect.model.enums.ComplaintStatus;
import com.civicconnect.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private DuplicateDetectionService duplicateDetectionService;

    private Category roadCategory;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(duplicateDetectionService, "similarityThreshold", 0.6);
        ReflectionTestUtils.setField(duplicateDetectionService, "proximityRadiusKm", 0.5);
        ReflectionTestUtils.setField(duplicateDetectionService, "maxResults", 5);

        roadCategory = new Category("Road Damage", "Road issues", "fas fa-road");
        roadCategory.setId(1L);
    }

    @Test
    void testFindPotentialDuplicates_DetectsHighSimilarityAndProximity() {
        Complaint newComplaint = new Complaint();
        newComplaint.setId(10L);
        newComplaint.setTitle("Huge Pothole on Oak Street");
        newComplaint.setDescription("Deep pothole in the middle lane causing dangerous bike skids.");
        newComplaint.setCategory(roadCategory);
        newComplaint.setLatitude(12.9716);
        newComplaint.setLongitude(77.5946);

        Complaint existing = new Complaint();
        existing.setId(5L);
        existing.setTitle("Large Pothole on Oak Street");
        existing.setDescription("Deep road pothole near Oak Street lane causing two-wheelers to fall.");
        existing.setCategory(roadCategory);
        existing.setStatus(ComplaintStatus.IN_PROGRESS);
        existing.setLatitude(12.9718);
        existing.setLongitude(77.5948);

        when(complaintRepository.findByCategoryExcluding(eq(roadCategory), eq(10L)))
                .thenReturn(List.of(existing));

        List<DuplicateCandidate> duplicates = duplicateDetectionService.findPotentialDuplicates(newComplaint);

        assertNotNull(duplicates);
        assertEquals(1, duplicates.size());
        DuplicateCandidate match = duplicates.get(0);
        assertEquals(5L, match.getComplaint().getId());
        assertTrue(match.getSimilarityScore() >= 0.6, "Similarity score should be above threshold");
        assertFalse(match.getMatchReasons().isEmpty(), "Match reasons should explain the detection");
    }

    @Test
    void testHaversineDistance_CalculatesAccurateDistance() {
        // Bangalore Central to nearby point ~ 0.22 km
        double distance = duplicateDetectionService.haversineDistance(12.9716, 77.5946, 12.9730, 77.5955);
        assertTrue(distance > 0.1 && distance < 0.4, "Distance should be approximately 0.2 km");
    }

    @Test
    void testFindPotentialDuplicates_IgnoresRejectedComplaints() {
        Complaint newComplaint = new Complaint();
        newComplaint.setId(1L);
        newComplaint.setTitle("Pothole on Main Road");
        newComplaint.setDescription("Road surface damaged");
        newComplaint.setCategory(roadCategory);

        Complaint rejected = new Complaint();
        rejected.setId(2L);
        rejected.setTitle("Pothole on Main Road");
        rejected.setDescription("Road surface damaged");
        rejected.setCategory(roadCategory);
        rejected.setStatus(ComplaintStatus.REJECTED);

        when(complaintRepository.findByCategoryExcluding(eq(roadCategory), eq(1L)))
                .thenReturn(List.of(rejected));

        List<DuplicateCandidate> duplicates = duplicateDetectionService.findPotentialDuplicates(newComplaint);
        assertTrue(duplicates.isEmpty(), "Rejected complaints should not be suggested as duplicates");
    }
}
