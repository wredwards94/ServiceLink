package com.wesleyedwards.ServiceLink.Entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String status; // Open, In Progress, Resolved
    private String priority; // Low, Medium, High
    private String category; // e.g., Technical, Billing
//    @ManyToOne
//    private User requester;
    //    @ManyToOne
//    private User assignedTo; // Agent assigned to the ticket
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
    @CreationTimestamp
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
