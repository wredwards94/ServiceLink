export type TicketNoComments = {
  id: string;
  title: string;
  description: string;
  status: string;
  priority: string;
  category: string;
  assignedTo: string;
  requester: string;
  createdAt: string;
  updatedAt: string;
}

export type UserId = {
  id: string;
}


// private String title;
// private String description;
// private String status; // Open, In Progress, Resolved
// private String priority; // Low, Medium, High
// private String category; // e.g., Technical, Billing
// private User assignedTo; // Agent assigned to the ticket
// private User requester;
// private Timestamp createdAt;
// private Timestamp updatedAt;
// private List<Comment> comments;
