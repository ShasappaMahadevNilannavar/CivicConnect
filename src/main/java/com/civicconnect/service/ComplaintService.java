package com.civicconnect.service;

import com.civicconnect.dto.AssignmentRequest;
import com.civicconnect.dto.ComplaintRequest;
import com.civicconnect.dto.FeedbackRequest;
import com.civicconnect.dto.StatusUpdateRequest;
import com.civicconnect.model.*;
import com.civicconnect.model.enums.ComplaintStatus;
import com.civicconnect.model.enums.NotificationType;
import com.civicconnect.model.enums.Priority;
import com.civicconnect.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class ComplaintService {

    private static final Logger logger = LoggerFactory.getLogger(ComplaintService.class);

    private static final Map<ComplaintStatus, Set<ComplaintStatus>> VALID_TRANSITIONS = new EnumMap<>(ComplaintStatus.class);
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        VALID_TRANSITIONS.put(ComplaintStatus.SUBMITTED,
                EnumSet.of(ComplaintStatus.UNDER_REVIEW, ComplaintStatus.REJECTED));
        VALID_TRANSITIONS.put(ComplaintStatus.UNDER_REVIEW,
                EnumSet.of(ComplaintStatus.APPROVED, ComplaintStatus.REJECTED));
        VALID_TRANSITIONS.put(ComplaintStatus.APPROVED,
                EnumSet.of(ComplaintStatus.IN_PROGRESS));
        VALID_TRANSITIONS.put(ComplaintStatus.IN_PROGRESS,
                EnumSet.of(ComplaintStatus.RESOLVED_PENDING_VERIFICATION));
        VALID_TRANSITIONS.put(ComplaintStatus.RESOLVED_PENDING_VERIFICATION,
                EnumSet.of(ComplaintStatus.VERIFIED_RESOLVED, ComplaintStatus.IN_PROGRESS));
    }

    private final ComplaintRepository complaintRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final StatusHistoryRepository statusHistoryRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ComplaintAssignmentRepository complaintAssignmentRepository;
    private final ResolutionFeedbackRepository resolutionFeedbackRepository;

    public ComplaintService(ComplaintRepository complaintRepository,
                            CategoryRepository categoryRepository,
                            FileStorageService fileStorageService,
                            StatusHistoryRepository statusHistoryRepository,
                            NotificationService notificationService,
                            AuditLogService auditLogService,
                            UserRepository userRepository,
                            DepartmentRepository departmentRepository,
                            ComplaintAssignmentRepository complaintAssignmentRepository,
                            ResolutionFeedbackRepository resolutionFeedbackRepository) {
        this.complaintRepository = complaintRepository;
        this.categoryRepository = categoryRepository;
        this.fileStorageService = fileStorageService;
        this.statusHistoryRepository = statusHistoryRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.complaintAssignmentRepository = complaintAssignmentRepository;
        this.resolutionFeedbackRepository = resolutionFeedbackRepository;
    }

    public Complaint createComplaint(ComplaintRequest req, List<MultipartFile> images, User citizen) {
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + req.getCategoryId()));

        String referenceId = generateReferenceId();

        Complaint complaint = new Complaint();
        complaint.setReferenceId(referenceId);
        complaint.setTitle(req.getTitle());
        complaint.setDescription(req.getDescription());
        complaint.setUser(citizen);
        complaint.setCategory(category);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setPriority(Priority.MEDIUM);
        complaint.setLatitude(req.getLatitude());
        complaint.setLongitude(req.getLongitude());
        complaint.setLocationDescription(req.getLocationDescription());

        Complaint savedComplaint = complaintRepository.save(complaint);

        if (images != null) {
            for (MultipartFile imageFile : images) {
                if (imageFile != null && !imageFile.isEmpty()) {
                    String storedFileName = fileStorageService.storeFile(imageFile);
                    ComplaintImage image = new ComplaintImage();
                    image.setComplaint(savedComplaint);
                    image.setFileName(storedFileName);
                    image.setOriginalFileName(imageFile.getOriginalFilename());
                    image.setFilePath(fileStorageService.getFilePath(storedFileName).toString());
                    image.setContentType(imageFile.getContentType());
                    image.setFileSize(imageFile.getSize());
                    if (savedComplaint.getImages() == null) {
                        savedComplaint.setImages(new ArrayList<>());
                    }
                    savedComplaint.getImages().add(image);
                }
            }
            savedComplaint = complaintRepository.save(savedComplaint);
        }

        StatusHistory initialHistory = new StatusHistory();
        initialHistory.setComplaint(savedComplaint);
        initialHistory.setOldStatus(null);
        initialHistory.setNewStatus(ComplaintStatus.SUBMITTED);
        initialHistory.setNotes("Complaint submitted by citizen");
        initialHistory.setChangedBy(citizen);
        statusHistoryRepository.save(initialHistory);

        logger.info("Complaint created: {} by user {}", referenceId, citizen.getEmail());
        return savedComplaint;
    }

    public Optional<Complaint> findByReferenceId(String refId) {
        return complaintRepository.findByReferenceId(refId);
    }

    public Optional<Complaint> findById(Long id) {
        return complaintRepository.findById(id);
    }

    public Page<Complaint> getComplaintsForCitizen(User user, String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (search != null && !search.trim().isEmpty()) {
            return complaintRepository.searchByUser(user, search.trim(), pageable);
        }

        if (status != null && !status.trim().isEmpty()) {
            try {
                ComplaintStatus complaintStatus = ComplaintStatus.valueOf(status.toUpperCase());
                return complaintRepository.findByUserAndStatus(user, complaintStatus, pageable);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status filter: {}", status);
            }
        }

        return complaintRepository.findByUser(user, pageable);
    }

    public Page<Complaint> getAllComplaints(String search, String status, String category, String priority, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (search != null && !search.trim().isEmpty()) {
            return complaintRepository.searchAll(search.trim(), pageable);
        }

        ComplaintStatus statusEnum = null;
        Long categoryId = null;
        Priority priorityEnum = null;

        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = ComplaintStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status filter: {}", status);
            }
        }

        if (category != null && !category.trim().isEmpty()) {
            try {
                categoryId = Long.parseLong(category);
            } catch (NumberFormatException e) {
                logger.warn("Invalid category filter: {}", category);
            }
        }

        if (priority != null && !priority.trim().isEmpty()) {
            try {
                priorityEnum = Priority.valueOf(priority.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid priority filter: {}", priority);
            }
        }

        Category categoryEntity = null;
        if (categoryId != null) {
            categoryEntity = categoryRepository.findById(categoryId).orElse(null);
        }
        String searchKeyword = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return complaintRepository.findWithFilters(statusEnum, categoryEntity, priorityEnum, searchKeyword, pageable);
    }

    public Complaint updateStatus(Long complaintId, StatusUpdateRequest req, User admin) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with id: " + complaintId));

        ComplaintStatus newStatus = req.getStatus();
        if (newStatus == null) {
            throw new IllegalArgumentException("Invalid status provided");
        }

        ComplaintStatus oldStatus = complaint.getStatus();
        Set<ComplaintStatus> allowedTransitions = VALID_TRANSITIONS.get(oldStatus);
        if (allowedTransitions == null || !allowedTransitions.contains(newStatus)) {
            throw new IllegalArgumentException(
                    String.format("Invalid status transition from %s to %s", oldStatus, newStatus));
        }

        if (newStatus == ComplaintStatus.RESOLVED_PENDING_VERIFICATION
                && (req.getResolutionDescription() == null || req.getResolutionDescription().trim().isEmpty())) {
            throw new IllegalArgumentException("Resolution description is required when resolving a complaint");
        }

        complaint.setStatus(newStatus);
        Complaint updatedComplaint = complaintRepository.save(complaint);

        StatusHistory history = new StatusHistory();
        history.setComplaint(updatedComplaint);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        String notes = req.getNotes() != null ? req.getNotes() : "";
        if (newStatus == ComplaintStatus.RESOLVED_PENDING_VERIFICATION && req.getResolutionDescription() != null) {
            notes = notes.isEmpty() ? req.getResolutionDescription() : notes + " | Resolution: " + req.getResolutionDescription();
        }
        history.setNotes(notes);
        history.setChangedBy(admin);
        statusHistoryRepository.save(history);

        notificationService.notifyStatusChange(updatedComplaint, oldStatus, newStatus);

        auditLogService.log(admin, "STATUS_UPDATE", "Complaint", complaintId,
                String.format("Status changed from %s to %s", oldStatus, newStatus), null);

        logger.info("Complaint {} status updated from {} to {} by {}", complaint.getReferenceId(), oldStatus, newStatus, admin.getEmail());
        return updatedComplaint;
    }

    public void assignComplaint(Long complaintId, AssignmentRequest req, User admin) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with id: " + complaintId));

        Department department = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found with id: " + req.getDepartmentId()));

        ComplaintAssignment assignment = new ComplaintAssignment();
        assignment.setComplaint(complaint);
        assignment.setDepartment(department);
        assignment.setAssignedTo(req.getAssignedTo());
        assignment.setAssignedBy(admin);
        assignment.setNotes(req.getNotes());
        assignment.setAssignedAt(LocalDateTime.now());
        complaintAssignmentRepository.save(assignment);

        String notificationMessage = String.format(
                "Your complaint '%s' (Ref: %s) has been assigned to the %s department.",
                complaint.getTitle(), complaint.getReferenceId(), department.getName());
        notificationService.createNotification(
                complaint.getUser(),
                "Complaint Assigned",
                notificationMessage,
                NotificationType.ASSIGNMENT,
                complaint.getReferenceId()
        );

        auditLogService.log(admin, "ASSIGNMENT", "Complaint", complaintId,
                String.format("Assigned to department: %s, person: %s", department.getName(), req.getAssignedTo()), null);

        logger.info("Complaint {} assigned to {} by {}", complaint.getReferenceId(), department.getName(), admin.getEmail());
    }

    public void setPriority(Long complaintId, Priority priority, User admin) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with id: " + complaintId));

        Priority oldPriority = complaint.getPriority();
        complaint.setPriority(priority);
        complaintRepository.save(complaint);

        auditLogService.log(admin, "PRIORITY_CHANGE", "Complaint", complaintId,
                String.format("Priority changed from %s to %s", oldPriority, priority), null);

        logger.info("Complaint {} priority set to {} by {}", complaint.getReferenceId(), priority, admin.getEmail());
    }

    public void submitFeedback(Long complaintId, FeedbackRequest req, User citizen) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with id: " + complaintId));

        if (complaint.getStatus() != ComplaintStatus.RESOLVED_PENDING_VERIFICATION) {
            throw new IllegalArgumentException("Feedback can only be submitted for complaints pending verification");
        }

        if (!complaint.getUser().getId().equals(citizen.getId())) {
            throw new IllegalArgumentException("You can only submit feedback for your own complaints");
        }

        ResolutionFeedback feedback = new ResolutionFeedback();
        feedback.setComplaint(complaint);
        feedback.setUser(citizen);
        feedback.setResolved(req.isResolved());
        feedback.setComments(req.getComments());
        resolutionFeedbackRepository.save(feedback);

        ComplaintStatus oldStatus = complaint.getStatus();
        ComplaintStatus newStatus;
        String historyNotes;

        if (req.isResolved()) {
            newStatus = ComplaintStatus.VERIFIED_RESOLVED;
            historyNotes = "Resolution verified by citizen. Feedback: " + req.getComments();
        } else {
            newStatus = ComplaintStatus.IN_PROGRESS;
            historyNotes = "Resolution disputed by citizen. Feedback: " + req.getComments();
        }

        complaint.setStatus(newStatus);
        complaintRepository.save(complaint);

        StatusHistory history = new StatusHistory();
        history.setComplaint(complaint);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setNotes(historyNotes);
        history.setChangedBy(citizen);
        statusHistoryRepository.save(history);

        String adminNotificationTitle = req.isResolved() ? "Complaint Verified as Resolved" : "Complaint Resolution Disputed";
        String adminNotificationMessage = String.format(
                "Citizen %s has %s the resolution of complaint '%s' (Ref: %s). Comments: %s",
                citizen.getName(),
                req.isResolved() ? "verified" : "disputed",
                complaint.getTitle(),
                complaint.getReferenceId(),
                req.getComments()
        );
        notificationService.notifyAdmins(adminNotificationTitle, adminNotificationMessage,
                NotificationType.FEEDBACK, complaint.getReferenceId());

        auditLogService.log(citizen, "FEEDBACK_SUBMITTED", "Complaint", complaintId,
                String.format("Feedback: %s - %s", req.isResolved() ? "Resolved" : "Disputed", req.getComments()), null);

        logger.info("Feedback submitted for complaint {} by {}: resolved={}", complaint.getReferenceId(), citizen.getEmail(), req.isResolved());
    }

    public long countByStatus(ComplaintStatus status) {
        return complaintRepository.countByStatus(status);
    }

    public long countByUser(User user) {
        return complaintRepository.countByUser(user);
    }

    public long countByUserAndStatus(User user, ComplaintStatus status) {
        return complaintRepository.countByUserAndStatus(user, status);
    }

    public List<Complaint> findAllWithCoordinates() {
        return complaintRepository.findAllWithCoordinates();
    }

    private String generateReferenceId() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        StringBuilder randomPart = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            randomPart.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        String referenceId = "CC-" + datePart + "-" + randomPart;

        while (complaintRepository.findByReferenceId(referenceId).isPresent()) {
            randomPart = new StringBuilder(5);
            for (int i = 0; i < 5; i++) {
                randomPart.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            referenceId = "CC-" + datePart + "-" + randomPart;
        }

        return referenceId;
    }
}
