package com.civicconnect.repository;

import com.civicconnect.model.Complaint;
import com.civicconnect.model.ComplaintImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintImageRepository extends JpaRepository<ComplaintImage, Long> {
    List<ComplaintImage> findByComplaint(Complaint complaint);
}
