package com.civicconnect.controller;

import com.civicconnect.dto.ComplaintRequest;
import com.civicconnect.dto.FeedbackRequest;
import com.civicconnect.dto.ProfileUpdateRequest;
import com.civicconnect.model.Complaint;
import com.civicconnect.model.ResolutionFeedback;
import com.civicconnect.model.StatusHistory;
import com.civicconnect.model.User;
import com.civicconnect.model.enums.ComplaintStatus;
import com.civicconnect.repository.ResolutionFeedbackRepository;
import com.civicconnect.repository.StatusHistoryRepository;
import com.civicconnect.service.CategoryService;
import com.civicconnect.service.ComplaintService;
import com.civicconnect.service.NotificationService;
import com.civicconnect.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/citizen")
public class CitizenController {

    private final ComplaintService complaintService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final StatusHistoryRepository statusHistoryRepository;
    private final ResolutionFeedbackRepository resolutionFeedbackRepository;

    public CitizenController(ComplaintService complaintService,
                             CategoryService categoryService,
                             UserService userService,
                             NotificationService notificationService,
                             StatusHistoryRepository statusHistoryRepository,
                             ResolutionFeedbackRepository resolutionFeedbackRepository) {
        this.complaintService = complaintService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.statusHistoryRepository = statusHistoryRepository;
        this.resolutionFeedbackRepository = resolutionFeedbackRepository;
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
    public String dashboard(Model model, Principal principal) {
        User user = userService.getCurrentUser(principal);

        long total = complaintService.countByUser(user);
        long pending = complaintService.countByUserAndStatus(user, ComplaintStatus.SUBMITTED)
                + complaintService.countByUserAndStatus(user, ComplaintStatus.UNDER_REVIEW);
        long inProgress = complaintService.countByUserAndStatus(user, ComplaintStatus.IN_PROGRESS);
        long resolved = complaintService.countByUserAndStatus(user, ComplaintStatus.RESOLVED_PENDING_VERIFICATION)
                + complaintService.countByUserAndStatus(user, ComplaintStatus.VERIFIED_RESOLVED);

        Page<Complaint> recentComplaints = complaintService.getComplaintsForCitizen(user, null, null, 0, 5);

        model.addAttribute("totalComplaints", total);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("resolvedCount", resolved);
        model.addAttribute("recentComplaints", recentComplaints.getContent());

        return "citizen/dashboard";
    }

    @GetMapping("/complaints/new")
    public String newComplaintForm(Model model) {
        model.addAttribute("complaintRequest", new ComplaintRequest());
        model.addAttribute("categories", categoryService.findActive());
        return "citizen/new-complaint";
    }

    @PostMapping("/complaints")
    public String createComplaint(@Valid @ModelAttribute("complaintRequest") ComplaintRequest req,
                                  BindingResult bindingResult,
                                  @RequestParam(value = "images", required = false) MultipartFile[] images,
                                  Principal principal,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findActive());
            return "citizen/new-complaint";
        }

        try {
            List<MultipartFile> imageList = (images != null) ? Arrays.asList(images) : List.of();
            Complaint created = complaintService.createComplaint(req, imageList, user);
            redirectAttributes.addFlashAttribute("successMessage", "Complaint submitted successfully! Reference ID: " + created.getReferenceId());
            return "redirect:/citizen/complaints/" + created.getReferenceId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error submitting complaint: " + e.getMessage());
            model.addAttribute("categories", categoryService.findActive());
            return "citizen/new-complaint";
        }
    }

    @GetMapping("/complaints")
    public String listComplaints(@RequestParam(value = "search", required = false) String search,
                                 @RequestParam(value = "status", required = false) String status,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "size", defaultValue = "10") int size,
                                 Principal principal,
                                 Model model) {
        User user = userService.getCurrentUser(principal);
        Page<Complaint> complaints = complaintService.getComplaintsForCitizen(user, search, status, page, size);

        model.addAttribute("complaints", complaints);
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentStatus", status);
        model.addAttribute("statuses", ComplaintStatus.values());

        return "citizen/complaints";
    }

    @GetMapping("/complaints/{referenceId}")
    public String complaintDetail(@PathVariable("referenceId") String referenceId,
                                  Principal principal,
                                  Model model) {
        User user = userService.getCurrentUser(principal);
        Complaint complaint = complaintService.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + referenceId));

        // Ownership authorization check
        if (!complaint.getUser().getId().equals(user.getId())) {
            return "redirect:/citizen/complaints?error=unauthorized";
        }

        List<StatusHistory> statusHistory = statusHistoryRepository.findByComplaintOrderByCreatedAtAsc(complaint);
        ResolutionFeedback feedback = resolutionFeedbackRepository.findByComplaintAndUser(complaint, user).orElse(null);

        model.addAttribute("complaint", complaint);
        model.addAttribute("statusHistory", statusHistory);
        model.addAttribute("feedback", feedback);
        model.addAttribute("feedbackRequest", new FeedbackRequest());

        return "citizen/complaint-detail";
    }

    @PostMapping("/complaints/{referenceId}/feedback")
    public String submitFeedback(@PathVariable("referenceId") String referenceId,
                                 @ModelAttribute("feedbackRequest") FeedbackRequest req,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        Complaint complaint = complaintService.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + referenceId));

        try {
            complaintService.submitFeedback(complaint.getId(), req, user);
            redirectAttributes.addFlashAttribute("successMessage", "Thank you! Your resolution verification feedback has been recorded.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/citizen/complaints/" + referenceId;
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        ProfileUpdateRequest profileReq = new ProfileUpdateRequest();
        profileReq.setName(user.getName());
        profileReq.setPhone(user.getPhone());
        profileReq.setAddress(user.getAddress());

        model.addAttribute("user", user);
        model.addAttribute("profileUpdateRequest", profileReq);
        return "citizen/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest req,
                                BindingResult bindingResult,
                                Principal principal,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        User user = userService.getCurrentUser(principal);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "citizen/profile";
        }

        userService.updateProfile(user.getId(), req);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        return "redirect:/citizen/profile";
    }

    @GetMapping("/notifications")
    public String notifications(Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        model.addAttribute("notifications", notificationService.getUnreadNotifications(user));
        return "citizen/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable("id") Long id, Principal principal) {
        User user = userService.getCurrentUser(principal);
        notificationService.markAsRead(id, user);
        return "redirect:/citizen/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead(Principal principal) {
        User user = userService.getCurrentUser(principal);
        notificationService.markAllAsRead(user);
        return "redirect:/citizen/notifications";
    }
}
