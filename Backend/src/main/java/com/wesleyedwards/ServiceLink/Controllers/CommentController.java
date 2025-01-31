package com.wesleyedwards.ServiceLink.Controllers;

import com.wesleyedwards.ServiceLink.Dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.Service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/{ticketId}")
    public ResponseEntity<CommentResponseDto> addComment(@PathVariable Long ticketId,
                                                         @RequestParam UUID authorId,
                                                         @RequestBody CommentRequestDto commentRequest) {
        return ResponseEntity.ok(commentService.addCommentToTicket(ticketId, authorId, commentRequest));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<List<CommentResponseDto>> getCommentsForTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.getCommentsForTicket(ticketId));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok("Comment deleted successfully");
    }
}
