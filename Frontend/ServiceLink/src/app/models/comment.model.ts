export interface CommentResponse {
  id: number;
  authorId: string;
  authorName: string;
  ticketId: number;
  content: string;
  createdAt: string;
  /** Staff-only note. The backend filters these out entirely for a USER. */
  internal: boolean;
}

/**
 * Mirrors CommentRequestDto.
 *
 * `content` is @NotBlank and @Size(max = 500) — exceeding it fails validation
 * with a 400, so the composer counts down against COMMENT_MAX_LENGTH.
 *
 * `internal` is honoured only for staff: CommentServiceImpl stores
 * `actor.isStaff() && request.internal()`, so a USER setting it is ignored
 * rather than rejected.
 */
export interface CommentRequest {
  content: string;
  internal: boolean;
}

export const COMMENT_MAX_LENGTH = 500;
