/** Mirrors AttachmentResponseDto. `size` is bytes. */
export interface AttachmentResponse {
  id: number;
  filename: string;
  contentType: string;
  size: number;
  uploader: string;
  createdAt: string;
}
