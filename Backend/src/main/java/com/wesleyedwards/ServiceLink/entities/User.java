package com.wesleyedwards.ServiceLink.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.wesleyedwards.ServiceLink.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "user_table")
@NoArgsConstructor
@SoftDelete
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false)
    private UUID userId;

    @Embedded
    private Credentials credentials;

    @Embedded
    private Profile profile;

    @OneToMany(mappedBy = "assignedTo")
    @JsonManagedReference
//    @JsonIdentityInfo()
    private List<Ticket> assignedTickets;

    @OneToMany(mappedBy = "requester")
    @JsonManagedReference
    private List<Ticket> requestedTickets;

    @Column(nullable = false)
    private boolean isDisabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER; // default to USER
}
