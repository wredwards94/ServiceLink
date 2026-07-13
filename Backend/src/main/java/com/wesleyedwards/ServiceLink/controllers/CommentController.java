package com.wesleyedwards.ServiceLink.controllers;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins="*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/ticket/{ticketId}")
    public ResponseEntity<CommentResponseDto> addComment(@PathVariable Long ticketId,
                                                         @AuthenticationPrincipal UserPrincipal actor,
                                                         @Valid @RequestBody CommentRequestDto commentRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addCommentToTicket(ticketId, actor,
                commentRequest));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(@PathVariable Long commentId,
                                                            @Valid @RequestBody CommentRequestDto updatedComment,
                                                            @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.ok(commentService.updateComment(commentId, updatedComment, actor));
    }

//    Add comment searching using filters and/or keywords
    @GetMapping("ticket/{ticketId}/search")
    public ResponseEntity<Page<CommentResponseDto>> searchComments(
            @PathVariable Long ticketId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @AuthenticationPrincipal UserPrincipal actor) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(commentService.searchComments(ticketId, keyword, pageable, actor));
    }

//    Add Pagination for future
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<Page<CommentResponseDto>> getCommentsForTicket(
            @PathVariable Long ticketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @AuthenticationPrincipal UserPrincipal actor) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(commentService.getCommentsForTicket(ticketId, pageable, actor));
    }

//    Add logging for production issues
    /*private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

        @PostMapping("/{ticketId}")
        public ResponseEntity<CommentResponseDto> addComment(...) {
            logger.info("Adding comment to ticket {}", ticketId);
        ...
        }*/
}
