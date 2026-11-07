package com.civicconnect.controller.api;

import com.civicconnect.dto.ChatComplaintDto;
import com.civicconnect.dto.ChatRequest;
import com.civicconnect.dto.ChatResponse;
import com.civicconnect.dto.ComplaintRequest;
import com.civicconnect.model.Category;
import com.civicconnect.model.Complaint;
import com.civicconnect.model.User;
import com.civicconnect.model.enums.Role;
import com.civicconnect.repository.CategoryRepository;
import com.civicconnect.repository.ComplaintRepository;
import com.civicconnect.repository.UserRepository;
import com.civicconnect.service.ChatService;
import com.civicconnect.service.ComplaintService;
import com.civicconnect.service.UserService;
import com.civicconnect.util.Base64MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private static final Logger log = LoggerFactory.getLogger(ChatApiController.class);

    private final ChatService chatService;
    private final ComplaintService complaintService;
    private final ComplaintRepository complaintRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public ChatApiController(ChatService chatService,
                             ComplaintService complaintService,
                             ComplaintRepository complaintRepository,
                             CategoryRepository categoryRepository,
                             UserRepository userRepository,
                             UserService userService,
                             PasswordEncoder passwordEncoder) {
        this.chatService = chatService;
        this.complaintService = complaintService;
        this.complaintRepository = complaintRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Please ask a question.", null, "en", false));
        }
        ChatResponse response = chatService.processChat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/track/{refId}")
    public ResponseEntity<?> trackComplaint(@PathVariable("refId") String refId) {
        if (refId == null || refId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("found", false, "message", "Reference ID is required."));
        }
        String cleanId = refId.trim().toUpperCase();
        Optional<Complaint> opt = complaintRepository.findByReferenceId(cleanId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "found", false,
                    "referenceId", cleanId,
                    "message", "No complaint found with Reference ID: " + cleanId
            ));
        }

        Complaint c = opt.get();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("found", true);
        resp.put("referenceId", c.getReferenceId());
        resp.put("title", c.getTitle());
        resp.put("description", c.getDescription());
        resp.put("category", c.getCategory() != null ? c.getCategory().getName() : "General");
        resp.put("status", c.getStatus() != null ? c.getStatus().name() : "SUBMITTED");
        resp.put("priority", c.getPriority() != null ? c.getPriority().name() : "MEDIUM");
        resp.put("locationDescription", c.getLocationDescription() != null ? c.getLocationDescription() : "Location provided");
        resp.put("latitude", c.getLatitude());
        resp.put("longitude", c.getLongitude());
        resp.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "");
        resp.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategories() {
        List<Category> categories = categoryRepository.findByActiveTrue();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Category cat : categories) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", cat.getId());
            item.put("name", cat.getName());
            item.put("description", cat.getDescription());
            item.put("icon", cat.getIcon());
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/submit-complaint")
    public ResponseEntity<?> submitComplaint(@RequestBody ChatComplaintDto dto, Principal principal) {
        try {
            if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
                dto.setTitle("Civic issue reported via AI Chatbot");
            }
            if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
                dto.setDescription("Civic problem reported via Chatbot assistant.");
            }

            // 1. Resolve Category
            Category resolvedCategory = resolveCategory(dto.getCategory());

            // 2. Resolve Citizen User
            User citizen = resolveCitizenUser(principal);

            // 3. Create ComplaintRequest
            ComplaintRequest req = new ComplaintRequest();
            req.setTitle(dto.getTitle().trim());
            req.setDescription(dto.getDescription().trim());
            req.setCategoryId(resolvedCategory.getId());
            req.setLocationDescription(dto.getLocationDescription() != null && !dto.getLocationDescription().isBlank()
                    ? dto.getLocationDescription().trim() : "Reported via CivicBot");
            req.setLatitude(dto.getLatitude() != null ? dto.getLatitude() : 20.5937); // Default India center if no GPS
            req.setLongitude(dto.getLongitude() != null ? dto.getLongitude() : 78.9629);

            // 4. Handle Optional Image
            List<MultipartFile> imageFiles = new ArrayList<>();
            if (dto.getImageBase64() != null && !dto.getImageBase64().isBlank()) {
                try {
                    String base64Data = dto.getImageBase64();
                    String contentType = "image/jpeg";
                    String fileName = dto.getImageFileName() != null ? dto.getImageFileName() : "chatbot-upload.jpg";

                    if (base64Data.contains(",")) {
                        String header = base64Data.substring(0, base64Data.indexOf(","));
                        if (header.contains("image/png")) {
                            contentType = "image/png";
                            if (!fileName.endsWith(".png")) fileName += ".png";
                        } else if (header.contains("image/webp")) {
                            contentType = "image/webp";
                            if (!fileName.endsWith(".webp")) fileName += ".webp";
                        } else {
                            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")) fileName += ".jpg";
                        }
                        base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                    }

                    byte[] decodedBytes = Base64.getDecoder().decode(base64Data.trim());
                    if (decodedBytes.length > 0) {
                        imageFiles.add(new Base64MultipartFile(decodedBytes, "images", fileName, contentType));
                    }
                } catch (Exception ex) {
                    log.warn("Failed to decode attached chat image, continuing without image: {}", ex.getMessage());
                }
            }

            // 5. Submit via real service
            Complaint created = complaintService.createComplaint(req, imageFiles, citizen);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("referenceId", created.getReferenceId());
            resp.put("title", created.getTitle());
            resp.put("category", created.getCategory().getName());
            resp.put("status", created.getStatus().name());
            resp.put("location", created.getLocationDescription());
            resp.put("createdAt", created.getCreatedAt() != null ? created.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "");
            resp.put("message", "Your complaint has been successfully recorded in the municipal database!");

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Failed to create complaint from chatbot: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Unable to submit complaint: " + e.getMessage()
            ));
        }
    }

    private Category resolveCategory(String categoryName) {
        List<Category> allCategories = categoryRepository.findByActiveTrue();
        if (allCategories.isEmpty()) {
            allCategories = categoryRepository.findAll();
        }

        if (categoryName != null && !categoryName.isBlank()) {
            String cleanName = categoryName.trim().toLowerCase();
            for (Category cat : allCategories) {
                if (cat.getName().equalsIgnoreCase(cleanName)) {
                    return cat;
                }
            }
            for (Category cat : allCategories) {
                String nameLower = cat.getName().toLowerCase();
                if (cleanName.contains(nameLower) || nameLower.contains(cleanName)) {
                    return cat;
                }
            }
            if (cleanName.contains("road") || cleanName.contains("pothole") || cleanName.contains("street")) {
                for (Category cat : allCategories) {
                    if (cat.getName().toLowerCase().contains("road")) return cat;
                }
            }
            if (cleanName.contains("light") || cleanName.contains("lamp") || cleanName.contains("electric")) {
                for (Category cat : allCategories) {
                    if (cat.getName().toLowerCase().contains("light")) return cat;
                }
            }
            if (cleanName.contains("garb") || cleanName.contains("waste") || cleanName.contains("trash")) {
                for (Category cat : allCategories) {
                    if (cat.getName().toLowerCase().contains("garb")) return cat;
                }
            }
            if (cleanName.contains("water") || cleanName.contains("leak") || cleanName.contains("pipe")) {
                for (Category cat : allCategories) {
                    if (cat.getName().toLowerCase().contains("water")) return cat;
                }
            }
            if (cleanName.contains("drain") || cleanName.contains("sewer") || cleanName.contains("flood")) {
                for (Category cat : allCategories) {
                    if (cat.getName().toLowerCase().contains("drain")) return cat;
                }
            }
        }

        if (!allCategories.isEmpty()) {
            return allCategories.get(0);
        }

        Category fallback = new Category();
        fallback.setName("General Civic Issue");
        fallback.setDescription("Reported via CivicBot AI");
        fallback.setActive(true);
        return categoryRepository.save(fallback);
    }

    private User resolveCitizenUser(Principal principal) {
        if (principal != null) {
            try {
                User user = userService.getCurrentUser(principal);
                if (user != null) return user;
            } catch (Exception ignored) {}
        }

        Optional<User> optCitizen = userRepository.findByEmail("citizen@civicconnect.com");
        if (optCitizen.isPresent()) {
            return optCitizen.get();
        }

        List<User> citizenList = userRepository.findByRole(Role.CITIZEN);
        if (!citizenList.isEmpty()) {
            return citizenList.get(0);
        }

        List<User> anyUsers = userRepository.findAll();
        if (!anyUsers.isEmpty()) {
            return anyUsers.get(0);
        }

        User guest = new User();
        guest.setName("Community Citizen");
        guest.setEmail("citizen@civicconnect.com");
        guest.setPassword(passwordEncoder.encode("CivicConnect@2026"));
        guest.setRole(Role.CITIZEN);
        guest.setEnabled(true);
        return userRepository.save(guest);
    }
}
