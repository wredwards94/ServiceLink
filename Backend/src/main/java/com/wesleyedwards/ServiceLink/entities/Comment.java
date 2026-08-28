package com.wesleyedwards.ServiceLink.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jdk.jfr.BooleanFlag;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SoftDelete
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
//    @JoinColumn(name = "ticket_id", nullable = false)
    @JsonBackReference(value = "comments")
    private Ticket ticket;

    @ManyToOne
    @JsonBackReference(value = "commentAuthor")
    private User author; // Reference to the User who made the comment

    // length must match CommentRequestDto's @Size(max = 500). Without it
    // Hibernate emits varchar(255) and a 256-500 char comment passes bean
    // validation, then fails on INSERT as an unhandled 500.
    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @BooleanFlag
    private boolean internal = false;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.REMOVE)
    @JsonIgnore
    private List<Attachment> attachments;
}
