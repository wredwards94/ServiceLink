package com.wesleyedwards.ServiceLink.repositories;

import com.wesleyedwards.ServiceLink.entities.Ticket;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    // requesterId scopes results to one requester's tickets; null (staff) means unscoped.
    @Query("SELECT t FROM Ticket t WHERE t.status = :status " +
            "AND (:requesterId IS NULL OR t.requester.userId = :requesterId)")
    List<Ticket> findAllTicketsByStatus(@Param("status") TicketStatus status,
                                        @Param("requesterId") UUID requesterId);

    @Query("SELECT t FROM Ticket t WHERE t.priority = :priority " +
            "AND (:requesterId IS NULL OR t.requester.userId = :requesterId)")
    List<Ticket> findAllTicketsByPriority(@Param("priority") TicketPriority priority,
                                          @Param("requesterId") UUID requesterId);

    //This query is to handle pagination for large datasets
    @Query("SELECT t FROM Ticket t WHERE " +
            "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:requesterId IS NULL OR t.requester.userId = :requesterId)")
    Page<Ticket> searchByKeyword(@Param("keyword") String keyword,
                                 @Param("requesterId") UUID requesterId,
                                 Pageable pageable);

    //Advanced search query with pagination to handle large datasets
    @Query("SELECT t FROM Ticket t WHERE " +
            "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:priority IS NULL OR t.priority = :priority) " +
            "AND (:requesterId IS NULL OR t.requester.userId = :requesterId)")
    Page<Ticket> advancedSearch(@Param("keyword") String keyword,
                                @Param("status")TicketStatus status,
                                @Param("priority")TicketPriority priority,
                                @Param("requesterId") UUID requesterId,
                                Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.requester.userId = :requesterId")
    List<Ticket> findAllByRequester(UUID requesterId);

    @Query("SELECT t FROM Ticket t WHERE t.assignedTo.userId = :userId")
    List<Ticket>findAllByAssignedToUser(UUID userId);

    List<Ticket> findAllById(List<Long> ids);
}
