package com.civicconnect.repository;

import com.civicconnect.model.Complaint;
import com.civicconnect.model.ResolutionFeedback;
import com.civicconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResolutionFeedbackRepository extends JpaRepository<ResolutionFeedback, Long> {
    List<ResolutionFeedback> findByComplaint(Complaint complaint);
    Optional<ResolutionFeedback> findByComplaintAndUser(Complaint complaint, User user);
}
