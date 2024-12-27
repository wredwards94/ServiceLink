package com.wesleyedwards.ServiceLink.Entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "user_table")
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Embedded
    private Credentials credentials;

    @Embedded
    private Profile profile;

    @OneToMany(mappedBy = "assignedTo")
    @JsonManagedReference
    private List<Ticket> tickets;
}
