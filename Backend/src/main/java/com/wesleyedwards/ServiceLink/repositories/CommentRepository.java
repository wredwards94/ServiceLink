package com.wesleyedwards.ServiceLink.repositories;

import com.wesleyedwards.ServiceLink.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findAllByTicketId(Long ticketId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.ticket.id = :ticketId " +
            "AND LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Comment> searchByTicketAndKeyword(@Param("ticketId") Long ticketId,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.ticket.id = :ticketId " +
            "AND LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND c.internal = false")
    Page<Comment> searchByTicketAndKeywordAndInternalFalse(@Param("ticketId") Long ticketId,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);

    Page<Comment> findAllByTicketIdAndInternalFalse(Long ticketId, Pageable pageable);
}
