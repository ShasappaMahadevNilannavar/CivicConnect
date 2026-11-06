package com.civicconnect.service;

import com.civicconnect.model.Complaint;
import com.civicconnect.model.Notification;
import com.civicconnect.model.User;
import com.civicconnect.model.enums.ComplaintStatus;
import com.civicconnect.model.enums.NotificationType;
import com.civicconnect.model.enums.Role;
import com.civicconnect.repository.NotificationRepository;
import com.civicconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Notification createNotification(User user, String title, String message, NotificationType type, String referenceId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);
        logger.debug("Notification created for user {}: {}", user.getEmail(), title);
        return saved;
    }

    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Notification does not belong to this user");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        logger.debug("Notification {} marked as read for user {}", notificationId, user.getEmail());
    }

    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);
        logger.debug("All notifications marked as read for user {}", user.getEmail());
    }

    public void notifyStatusChange(Complaint complaint, ComplaintStatus oldStatus, ComplaintStatus newStatus) {
        String title = "Complaint Status Updated";
        String message = String.format("Your complaint '%s' (Ref: %s) status has changed from %s to %s.",
                complaint.getTitle(),
                complaint.getReferenceId(),
                oldStatus != null ? oldStatus.name() : "NEW",
                newStatus.name());

        createNotification(
                complaint.getUser(),
                title,
                message,
                NotificationType.STATUS_CHANGE,
                complaint.getReferenceId()
        );
    }

    public void notifyAdmins(String title, String message, NotificationType type, String referenceId) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            createNotification(admin, title, message, type, referenceId);
        }
        logger.debug("Notified {} admins: {}", admins.size(), title);
    }
}
