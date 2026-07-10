package com.wesleyedwards.ServiceLink.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@SoftDelete
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private TicketStatus status = TicketStatus.NEW;// Open, In Progress, Resolved

    @Enumerated(EnumType.STRING)
    private TicketPriority priority; // Low, Medium, High

    private String category; // e.g., Technical, Billing

    @ManyToOne
    @JsonBackReference(value = "assignedTickets")
    private User assignedTo; // Agent assigned to the ticket

    @ManyToOne
    @JsonBackReference(value = "requestedTickets")
    private User requester;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.REMOVE)
    @JsonManagedReference(value = "commentAuthor")
    private List<Comment> comments;
}
