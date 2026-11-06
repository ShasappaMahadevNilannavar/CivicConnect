package com.civicconnect.service;

import com.civicconnect.dto.FeedbackRequest;
import com.civicconnect.dto.StatusUpdateRequest;
import com.civicconnect.model.Category;
import com.civicconnect.model.Complaint;
import com.civicconnect.model.StatusHistory;
import com.civicconnect.model.User;
import com.civicconnect.model.enums.ComplaintStatus;
import com.civicconnect.model.enums.Role;
import com.civicconnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ComplaintAssignmentRepository complaintAssignmentRepository;

    @Mock
    private ResolutionFeedbackRepository resolutionFeedbackRepository;

    @InjectMocks
    private ComplaintService complaintService;

    private User citizen;
    private User otherCitizen;
    private User admin;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        citizen = new User();
        citizen.setId(1L);
        citizen.setName("Citizen Jane");
        citizen.setEmail("jane@example.com");
        citizen.setRole(Role.CITIZEN);

        otherCitizen = new User();
        otherCitizen.setId(2L);
        otherCitizen.setName("Impostor");
        otherCitizen.setEmail("impostor@example.com");
        otherCitizen.setRole(Role.CITIZEN);

        admin = new User();
        admin.setId(99L);
        admin.setName("Admin Officer");
        admin.setEmail("admin@civicconnect.com");
        admin.setRole(Role.ADMIN);

        complaint = new Complaint();
        complaint.setId(10L);
        complaint.setReferenceId("CC-20261101-A101");
        complaint.setTitle("Leaking Pipe");
        complaint.setDescription("Fresh water pipe leaking on main sidewalk");
        complaint.setUser(citizen);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
    }

    @Test
    void testUpdateStatus_ValidTransition_SubmittedToUnderReview() {
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus(ComplaintStatus.UNDER_REVIEW);
        req.setNotes("Assigned inspector to examine site.");

        Complaint updated = complaintService.updateStatus(10L, req, admin);

        assertEquals(ComplaintStatus.UNDER_REVIEW, updated.getStatus());
        verify(statusHistoryRepository, times(1)).save(any(StatusHistory.class));
        verify(notificationService, times(1)).notifyStatusChange(eq(complaint), eq(ComplaintStatus.SUBMITTED), eq(ComplaintStatus.UNDER_REVIEW));
    }

    @Test
    void testUpdateStatus_InvalidDirectTransition_ThrowsException() {
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));

        // Direct skip from SUBMITTED to RESOLVED_PENDING_VERIFICATION is forbidden
        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus(ComplaintStatus.RESOLVED_PENDING_VERIFICATION);
        req.setNotes("Trying to bypass workflow");

        assertThrows(IllegalArgumentException.class, () -> complaintService.updateStatus(10L, req, admin));
        verify(complaintRepository, never()).save(any(Complaint.class));
    }

    @Test
    void testUpdateStatus_ResolvingRequiresResolutionDescription() {
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus(ComplaintStatus.RESOLVED_PENDING_VERIFICATION);
        req.setNotes("Work finished");
        req.setResolutionDescription(""); // Empty resolution description

        assertThrows(IllegalArgumentException.class, () -> complaintService.updateStatus(10L, req, admin));
    }

    @Test
    void testSubmitFeedback_CitizenVerifiesResolution_StatusBecomesVerifiedResolved() {
        complaint.setStatus(ComplaintStatus.RESOLVED_PENDING_VERIFICATION);
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));

        FeedbackRequest req = new FeedbackRequest();
        req.setResolved(true);
        req.setComments("Inspected this morning. Pipe is fixed and asphalt repaved cleanly!");

        complaintService.submitFeedback(10L, req, citizen);

        assertEquals(ComplaintStatus.VERIFIED_RESOLVED, complaint.getStatus());
        verify(resolutionFeedbackRepository, times(1)).save(any());
        verify(statusHistoryRepository, times(1)).save(any());
    }

    @Test
    void testSubmitFeedback_CitizenDisputesResolution_StatusRevertsToInProgress() {
        complaint.setStatus(ComplaintStatus.RESOLVED_PENDING_VERIFICATION);
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));

        FeedbackRequest req = new FeedbackRequest();
        req.setResolved(false);
        req.setComments("Water is still oozing through the gravel. Issue not solved.");

        complaintService.submitFeedback(10L, req, citizen);

        assertEquals(ComplaintStatus.IN_PROGRESS, complaint.getStatus());
        verify(resolutionFeedbackRepository, times(1)).save(any());
    }

    @Test
    void testSubmitFeedback_NonOwnerCannotSubmitFeedback_ThrowsSecurityException() {
        complaint.setStatus(ComplaintStatus.RESOLVED_PENDING_VERIFICATION);
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));

        FeedbackRequest req = new FeedbackRequest();
        req.setResolved(true);
        req.setComments("Trying to close someone else's complaint");

        assertThrows(IllegalArgumentException.class, () -> complaintService.submitFeedback(10L, req, otherCitizen));
    }
}
