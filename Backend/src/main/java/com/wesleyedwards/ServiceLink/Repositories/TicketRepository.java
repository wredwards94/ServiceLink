package com.wesleyedwards.ServiceLink.Repositories;

import com.wesleyedwards.ServiceLink.Entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findAllTicketsByStatus(String status);
    List<Ticket> findAllTicketsByPriority(String priority);

    //This query is to handle pagination for large datasets
    @Query("SELECT t FROM Ticket t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Ticket> searchByKeyword(String keyword);

    //Advanced search query
    @Query("SELECT t FROM Ticket t WHERE " +
            "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:priority IS NULL OR t.priority = :priority)")
    List<Ticket> advancedSearch(String keyword, String status, String priority);

    @Query("SELECT t FROM Ticket t WHERE t.requester.userId = :requesterId")
    List<Ticket> findAllByRequester(UUID requesterId);

    @Query("SELECT t FROM Ticket t WHERE t.assignedTo.userId = :userId")
    List<Ticket>findAllByAssignedToUser(UUID userId);
}
