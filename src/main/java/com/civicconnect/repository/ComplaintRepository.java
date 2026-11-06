package com.civicconnect.repository;

import com.civicconnect.model.Category;
import com.civicconnect.model.Complaint;
import com.civicconnect.model.User;
import com.civicconnect.model.enums.ComplaintStatus;
import com.civicconnect.model.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByReferenceId(String referenceId);

    Page<Complaint> findByUser(User user, Pageable pageable);

    Page<Complaint> findByUserAndStatus(User user, ComplaintStatus status, Pageable pageable);

    Page<Complaint> findByUserAndCategory(User user, Category category, Pageable pageable);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    Page<Complaint> findByCategory(Category category, Pageable pageable);

    Page<Complaint> findByPriority(Priority priority, Pageable pageable);

    @Query("SELECT c FROM Complaint c WHERE c.user = :user AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.referenceId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Complaint> searchByUser(@Param("user") User user, @Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Complaint c WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.referenceId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Complaint> searchAll(@Param("search") String search, Pageable pageable);

    long countByStatus(ComplaintStatus status);

    long countByUser(User user);

    long countByUserAndStatus(User user, ComplaintStatus status);

    @Query("SELECT c FROM Complaint c WHERE c.category = :category AND c.status != com.civicconnect.model.enums.ComplaintStatus.REJECTED AND c.id != :excludeId")
    List<Complaint> findByCategoryExcluding(@Param("category") Category category, @Param("excludeId") Long excludeId);

    @Query("SELECT c FROM Complaint c JOIN FETCH c.category WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND c.status != com.civicconnect.model.enums.ComplaintStatus.REJECTED")
    List<Complaint> findAllWithCoordinates();

    @Query("SELECT c FROM Complaint c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:category IS NULL OR c.category = :category) AND " +
           "(:priority IS NULL OR c.priority = :priority) AND " +
           "(:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.referenceId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Complaint> findWithFilters(@Param("status") ComplaintStatus status,
                                     @Param("category") Category category,
                                     @Param("priority") Priority priority,
                                     @Param("search") String search,
                                     Pageable pageable);
}
