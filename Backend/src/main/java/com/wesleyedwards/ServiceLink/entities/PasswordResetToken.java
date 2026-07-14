package com.wesleyedwards.ServiceLink.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "password_reset_token")
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    // Valid-by-construction: both token and expiresAt are non-null columns, so build a
    // single-use token for the given user with a fresh random value and a TTL from now.
    public static PasswordResetToken issueFor(User user, Duration ttl) {
        PasswordResetToken token = new PasswordResetToken();
        token.token = UUID.randomUUID().toString();
        token.user = user;
        token.expiresAt = LocalDateTime.now().plus(ttl);
        return token;
    }
}
