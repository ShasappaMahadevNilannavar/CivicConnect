package com.civicconnect.controller;

import com.civicconnect.model.User;
import com.civicconnect.model.enums.ComplaintStatus;
import com.civicconnect.service.CategoryService;
import com.civicconnect.service.NotificationService;
import com.civicconnect.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@Controller
public class MapController {

    private final CategoryService categoryService;
    private final NotificationService notificationService;
    private final UserService userService;

    public MapController(CategoryService categoryService,
                         NotificationService notificationService,
                         UserService userService) {
        this.categoryService = categoryService;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @ModelAttribute("unreadNotificationCount")
    public long getUnreadNotificationCount(Principal principal) {
        if (principal == null) return 0;
        User user = userService.getCurrentUser(principal);
        return notificationService.getUnreadCount(user);
    }

    @GetMapping("/map")
    public String publicMap(Model model) {
        model.addAttribute("categories", categoryService.findActive());
        model.addAttribute("statuses", ComplaintStatus.values());
        return "map";
    }

    @GetMapping("/map/3d")
    public String map3D(Model model) {
        model.addAttribute("categories", categoryService.findActive());
        return "map3d";
    }
}
