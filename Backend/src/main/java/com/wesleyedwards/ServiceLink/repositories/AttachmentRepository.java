package com.wesleyedwards.ServiceLink.repositories;

import com.wesleyedwards.ServiceLink.dtos.AttachmentResponseDto;
import com.wesleyedwards.ServiceLink.entities.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    // Selects metadata only — loading the entity would pull the bytea column for every row.
    @Query("select new com.wesleyedwards.ServiceLink.dtos.AttachmentResponseDto("
            + "a.id, a.filename, a.contentType, a.size, a.uploader.userId, a.createdAt) "
            + "from Attachment a where a.ticket.id = :ticketId order by a.createdAt asc")
    List<AttachmentResponseDto> listForTicket(@Param("ticketId") Long ticketId);

    @Query("select new com.wesleyedwards.ServiceLink.dtos.AttachmentResponseDto("
            + "a.id, a.filename, a.contentType, a.size, a.uploader.userId, a.createdAt) "
            + "from Attachment a where a.comment.id = :commentId order by a.createdAt asc")
    List<AttachmentResponseDto> listForComment(@Param("commentId") Long commentId);
}
