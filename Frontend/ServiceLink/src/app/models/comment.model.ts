export interface CommentResponse {
  id: number,
  authorId: number,
  authorName: string,
  ticketId: number,
  content: string,
  createdAt: string
}

export interface CommentRequest {
  content: string
}

/*
@NotBlank(message = "Content is required")
@Size(max = 500, message = "Comment cannot exceed 500 characters")
String content
 */
