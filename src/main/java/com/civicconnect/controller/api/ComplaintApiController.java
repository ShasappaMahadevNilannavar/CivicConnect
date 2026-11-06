package com.civicconnect.controller.api;

import com.civicconnect.model.Complaint;
import com.civicconnect.model.StatusHistory;
import com.civicconnect.model.User;
import com.civicconnect.model.enums.ComplaintStatus;
import com.civicconnect.repository.ComplaintRepository;
import com.civicconnect.repository.StatusHistoryRepository;
import com.civicconnect.service.NotificationService;
import com.civicconnect.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class ComplaintApiController {

    private final ComplaintRepository complaintRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final NotificationService notificationService;
    private final UserService userService;

    public ComplaintApiController(ComplaintRepository complaintRepository,
                                  StatusHistoryRepository statusHistoryRepository,
                                  NotificationService notificationService,
                                  UserService userService) {
        this.complaintRepository = complaintRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping("/complaints/map")
    public ResponseEntity<List<Map<String, Object>>> getMapComplaints(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status) {

        List<Complaint> complaints = complaintRepository.findAllWithCoordinates();
        List<Map<String, Object>> response = new ArrayList<>();

        for (Complaint c : complaints) {
            String catName = c.getCategory() != null ? c.getCategory().getName() : "General";
            if (category != null && !category.isEmpty() && !catName.equalsIgnoreCase(category)) {
                continue;
            }
            if (status != null && !status.isEmpty()) {
                try {
                    ComplaintStatus statusEnum = ComplaintStatus.valueOf(status.toUpperCase());
                    if (c.getStatus() != statusEnum) continue;
                } catch (IllegalArgumentException ignored) {}
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("referenceId", c.getReferenceId());
            item.put("title", c.getTitle());
            item.put("category", catName);
            item.put("categoryIcon", (c.getCategory() != null && c.getCategory().getIcon() != null) ? c.getCategory().getIcon() : "bi-geo-alt");
            item.put("status", c.getStatus() != null ? c.getStatus().name() : "SUBMITTED");
            item.put("priority", c.getPriority() != null ? c.getPriority().name() : "UNASSIGNED");
            item.put("latitude", c.getLatitude());
            item.put("longitude", c.getLongitude());
            item.put("locationDescription", c.getLocationDescription() != null ? c.getLocationDescription() : "");
            item.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "");

            response.add(item);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/complaints/{id}/timeline")
    public ResponseEntity<List<Map<String, Object>>> getComplaintTimeline(@PathVariable("id") Long id) {
        Complaint complaint = complaintRepository.findById(id).orElse(null);
        if (complaint == null) {
            return ResponseEntity.notFound().build();
        }

        List<StatusHistory> historyList = statusHistoryRepository.findByComplaintOrderByCreatedAtAsc(complaint);
        List<Map<String, Object>> result = new ArrayList<>();

        for (StatusHistory h : historyList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("oldStatus", h.getOldStatus() != null ? h.getOldStatus().name() : null);
            map.put("newStatus", h.getNewStatus().name());
            map.put("notes", h.getNotes());
            map.put("changedBy", h.getChangedBy() != null ? h.getChangedBy().getName() : "System");
            map.put("createdAt", h.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/notifications/count")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("count", 0L));
        }
        User user = userService.getCurrentUser(principal);
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Map<String, String>> markRead(@PathVariable("id") Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getCurrentUser(principal);
        notificationService.markAsRead(id, user);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getCurrentUser(principal);
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
