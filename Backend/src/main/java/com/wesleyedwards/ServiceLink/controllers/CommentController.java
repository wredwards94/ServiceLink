package com.wesleyedwards.ServiceLink.controllers;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins="*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/ticket/{ticketId}")
    public ResponseEntity<CommentResponseDto> addComment(@PathVariable Long ticketId,
                                                         @AuthenticationPrincipal UserPrincipal user,
                                                         @Valid @RequestBody CommentRequestDto commentRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addCommentToTicket(ticketId, user.getUserId(),
                commentRequest));
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<CommentResponseDto>> getCommentsForTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.getCommentsForTicket(ticketId));
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
    /*@GetMapping("/{ticketId}/search")
    public ResponseEntity<List<CommentResponseDto>> searchComments(
            @PathVariable Long ticketId,
            @RequestParam String keyword) {
        return ResponseEntity.ok(commentService.searchComments(ticketId, keyword));
    }*/

//    Add Pagination for future
    /*@GetMapping("/{ticketId}")
    public ResponseEntity<List<CommentResponseDto>> getCommentsForTicket(
            @PathVariable Long ticketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commentService.getCommentsForTicket(ticketId, page, size));
    }*/

//    Add logging for production issues
    /*private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

        @PostMapping("/{ticketId}")
        public ResponseEntity<CommentResponseDto> addComment(...) {
            logger.info("Adding comment to ticket {}", ticketId);
        ...
        }*/
}
