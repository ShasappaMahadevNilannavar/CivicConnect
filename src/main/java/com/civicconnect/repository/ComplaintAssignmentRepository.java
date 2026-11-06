package com.civicconnect.repository;

import com.civicconnect.model.Complaint;
import com.civicconnect.model.ComplaintAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintAssignmentRepository extends JpaRepository<ComplaintAssignment, Long> {
    List<ComplaintAssignment> findByComplaintOrderByAssignedAtDesc(Complaint complaint);
    Optional<ComplaintAssignment> findTopByComplaintOrderByAssignedAtDesc(Complaint complaint);
}
