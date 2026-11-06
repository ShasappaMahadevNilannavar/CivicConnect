package com.civicconnect.controller;

import com.civicconnect.dto.AssignmentRequest;
import com.civicconnect.dto.DuplicateCandidate;
import com.civicconnect.dto.StatusUpdateRequest;
import com.civicconnect.model.*;
import com.civicconnect.model.enums.ComplaintStatus;
import com.civicconnect.model.enums.Priority;
import com.civicconnect.repository.*;
import com.civicconnect.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ComplaintService complaintService;
    private final CategoryService categoryService;
    private final DepartmentService departmentService;
    private final UserService userService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;
    private final ComplaintRepository complaintRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final ComplaintAssignmentRepository complaintAssignmentRepository;
    private final ResolutionFeedbackRepository resolutionFeedbackRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminController(ComplaintService complaintService,
                           CategoryService categoryService,
                           DepartmentService departmentService,
                           UserService userService,
                           DuplicateDetectionService duplicateDetectionService,
                           NotificationService notificationService,
                           AuditLogService auditLogService,
                           FileStorageService fileStorageService,
                           ComplaintRepository complaintRepository,
                           StatusHistoryRepository statusHistoryRepository,
                           ComplaintAssignmentRepository complaintAssignmentRepository,
                           ResolutionFeedbackRepository resolutionFeedbackRepository,
                           AuditLogRepository auditLogRepository) {
        this.complaintService = complaintService;
        this.categoryService = categoryService;
        this.departmentService = departmentService;
        this.userService = userService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.fileStorageService = fileStorageService;
        this.complaintRepository = complaintRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.complaintAssignmentRepository = complaintAssignmentRepository;
        this.resolutionFeedbackRepository = resolutionFeedbackRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @ModelAttribute("unreadNotificationCount")
    public long getUnreadNotificationCount(Principal principal) {
        if (principal == null) return 0;
        User user = userService.getCurrentUser(principal);
        return notificationService.getUnreadCount(user);
    }

    @ModelAttribute("currentUser")
    public User getCurrentUser(Principal principal) {
        if (principal == null) return null;
        return userService.getCurrentUser(principal);
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalComplaints = complaintRepository.count();
        long submittedCount = complaintService.countByStatus(ComplaintStatus.SUBMITTED);
        long underReviewCount = complaintService.countByStatus(ComplaintStatus.UNDER_REVIEW);
        long approvedCount = complaintService.countByStatus(ComplaintStatus.APPROVED);
        long inProgressCount = complaintService.countByStatus(ComplaintStatus.IN_PROGRESS);
        long resolvedPendingCount = complaintService.countByStatus(ComplaintStatus.RESOLVED_PENDING_VERIFICATION);
        long verifiedResolvedCount = complaintService.countByStatus(ComplaintStatus.VERIFIED_RESOLVED);
        long resolvedCount = resolvedPendingCount + verifiedResolvedCount;
        long rejectedCount = complaintService.countByStatus(ComplaintStatus.REJECTED);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("Submitted", submittedCount);
        statusCounts.put("Under Review", underReviewCount);
        statusCounts.put("Approved", approvedCount);
        statusCounts.put("In Progress", inProgressCount);
        statusCounts.put("Resolved Pending", resolvedPendingCount);
        statusCounts.put("Verified Resolved", verifiedResolvedCount);
        statusCounts.put("Rejected", rejectedCount);

        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        for (Category cat : categoryService.findAll()) {
            Page<Complaint> catComplaints = complaintRepository.findByCategory(cat, PageRequest.of(0, 1));
            categoryCounts.put(cat.getName(), catComplaints.getTotalElements());
        }

        Map<String, Long> priorityCounts = new LinkedHashMap<>();
        for (Priority p : Priority.values()) {
            priorityCounts.put(p.name(), complaintRepository.findByPriority(p, PageRequest.of(0, 1)).getTotalElements());
        }

        Page<Complaint> recentComplaints = complaintRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("totalComplaints", totalComplaints);
        model.addAttribute("submittedCount", submittedCount);
        model.addAttribute("underReviewCount", underReviewCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("priorityCounts", priorityCounts);
        model.addAttribute("recentComplaints", recentComplaints.getContent());

        return "admin/dashboard";
    }

    @GetMapping("/complaints")
    public String listComplaints(@RequestParam(value = "search", required = false) String search,
                                 @RequestParam(value = "status", required = false) String status,
                                 @RequestParam(value = "category", required = false) String category,
                                 @RequestParam(value = "priority", required = false) String priority,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "size", defaultValue = "10") int size,
                                 Model model) {
        Page<Complaint> complaints = complaintService.getAllComplaints(search, status, category, priority, page, size);

        model.addAttribute("complaints", complaints);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("statuses", ComplaintStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentCategory", category);
        model.addAttribute("currentPriority", priority);

        return "admin/complaints";
    }

    @GetMapping("/complaints/{referenceId}")
    public String complaintDetail(@PathVariable("referenceId") String referenceId, Model model) {
        Complaint complaint = complaintService.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + referenceId));

        List<DuplicateCandidate> duplicates = duplicateDetectionService.findPotentialDuplicates(complaint);
        List<StatusHistory> statusHistory = statusHistoryRepository.findByComplaintOrderByCreatedAtAsc(complaint);
        List<ComplaintAssignment> assignments = complaintAssignmentRepository.findByComplaintOrderByAssignedAtDesc(complaint);
        List<ResolutionFeedback> feedbackList = resolutionFeedbackRepository.findByComplaint(complaint);
        List<AuditLog> auditLogs = auditLogService.getLogsForEntity("Complaint", complaint.getId());

        model.addAttribute("complaint", complaint);
        model.addAttribute("duplicates", duplicates);
        model.addAttribute("statusHistory", statusHistory);
        model.addAttribute("assignments", assignments);
        model.addAttribute("feedbackList", feedbackList);
        model.addAttribute("auditLogs", auditLogs);
        model.addAttribute("categories", categoryService.findActive());
        model.addAttribute("departments", departmentService.findActive());
        model.addAttribute("statuses", ComplaintStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statusUpdateRequest", new StatusUpdateRequest());
        model.addAttribute("assignmentRequest", new AssignmentRequest());

        return "admin/complaint-detail";
    }

    @PostMapping("/complaints/{referenceId}/status")
    public String updateStatus(@PathVariable("referenceId") String referenceId,
                               @Valid @ModelAttribute("statusUpdateRequest") StatusUpdateRequest req,
                               BindingResult bindingResult,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        User admin = userService.getCurrentUser(principal);
        Complaint complaint = complaintService.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + referenceId));

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Status and notes are required for all status transitions.");
            return "redirect:/admin/complaints/" + referenceId;
        }

        try {
            complaintService.updateStatus(complaint.getId(), req, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Status successfully updated to " + req.getStatus());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/complaints/" + referenceId;
    }

    @PostMapping("/complaints/{referenceId}/priority")
    public String updatePriority(@PathVariable("referenceId") String referenceId,
                                 @RequestParam("priority") Priority priority,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        User admin = userService.getCurrentUser(principal);
        Complaint complaint = complaintService.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + referenceId));

        try {
            complaintService.setPriority(complaint.getId(), priority, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Priority updated to " + priority);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/complaints/" + referenceId;
    }

    @PostMapping("/complaints/{referenceId}/assign")
    public String assignComplaint(@PathVariable("referenceId") String referenceId,
                                  @ModelAttribute("assignmentRequest") AssignmentRequest req,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        User admin = userService.getCurrentUser(principal);
        Complaint complaint = complaintService.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + referenceId));

        try {
            complaintService.assignComplaint(complaint.getId(), req, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Complaint successfully assigned to department / personnel.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/complaints/" + referenceId;
    }

    @PostMapping("/complaints/{referenceId}/evidence")
    public String uploadEvidence(@PathVariable("referenceId") String referenceId,
                                 @RequestParam("evidenceImages") MultipartFile[] files,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        User admin = userService.getCurrentUser(principal);
        Complaint complaint = complaintService.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + referenceId));

        try {
            int uploadedCount = 0;
            if (files != null) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String storedFileName = fileStorageService.storeFile(file);
                        ComplaintImage img = new ComplaintImage();
                        img.setComplaint(complaint);
                        img.setFileName(storedFileName);
                        img.setOriginalFileName(file.getOriginalFilename());
                        img.setFilePath(fileStorageService.getFilePath(storedFileName).toString());
                        img.setContentType(file.getContentType());
                        img.setFileSize(file.getSize());

                        if (complaint.getImages() == null) {
                            complaint.setImages(new ArrayList<>());
                        }
                        complaint.getImages().add(img);
                        uploadedCount++;
                    }
                }
                complaintRepository.save(complaint);
            }

            auditLogService.log(admin, "EVIDENCE_UPLOAD", "Complaint", complaint.getId(),
                    "Uploaded " + uploadedCount + " post-repair resolution evidence image(s)", null);

            redirectAttributes.addFlashAttribute("successMessage", "Uploaded " + uploadedCount + " resolution evidence file(s).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error uploading evidence: " + e.getMessage());
        }

        return "redirect:/admin/complaints/" + referenceId;
    }

    @GetMapping("/duplicates")
    public String reviewDuplicates(Model model) {
        List<Complaint> activeComplaints = complaintRepository.findAll();
        Map<Complaint, List<DuplicateCandidate>> duplicateMap = new LinkedHashMap<>();

        for (Complaint c : activeComplaints) {
            if (c.getStatus() != ComplaintStatus.REJECTED && c.getStatus() != ComplaintStatus.VERIFIED_RESOLVED) {
                List<DuplicateCandidate> dupes = duplicateDetectionService.findPotentialDuplicates(c);
                if (!dupes.isEmpty()) {
                    duplicateMap.put(c, dupes);
                }
            }
        }

        model.addAttribute("duplicateMap", duplicateMap);
        return "admin/duplicates";
    }
}
