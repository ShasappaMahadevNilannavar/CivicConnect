package com.civicconnect.repository;

import com.civicconnect.model.Complaint;
import com.civicconnect.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByComplaintOrderByCreatedAtAsc(Complaint complaint);
}
