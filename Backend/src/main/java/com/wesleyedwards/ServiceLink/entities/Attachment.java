package com.wesleyedwards.ServiceLink.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SoftDelete
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String contentType;
    private long size;
    private byte[] data;

    @ManyToOne
    @JsonIgnore
    private Ticket ticket;

    @ManyToOne
    @JsonIgnore
    private Comment comment;

    @ManyToOne
    @JsonIgnore
    private User uploader;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
