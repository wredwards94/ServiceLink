package com.wesleyedwards.ServiceLink.Controllers;

import com.wesleyedwards.ServiceLink.Dtos.CommentRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.CommentResponseDto;
import com.wesleyedwards.ServiceLink.Service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins="*")
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

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(@PathVariable Long commentId,
                                                            @RequestBody CommentRequestDto updatedComment) {
        return ResponseEntity.ok(commentService.updateComment(commentId, updatedComment));
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
