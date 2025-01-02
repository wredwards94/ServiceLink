package com.wesleyedwards.ServiceLink.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Credentials {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;
}
