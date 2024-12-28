package com.wesleyedwards.ServiceLink.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.List;

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

    @ManyToOne
    @JsonBackReference(value = "assignedTickets")
    private User assignedTo; // Agent assigned to the ticket

    @ManyToOne
    @JsonBackReference(value = "requestedTickets")
    private User requester;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "ticket")
    @JsonManagedReference
    private List<Comment> comments;
}
