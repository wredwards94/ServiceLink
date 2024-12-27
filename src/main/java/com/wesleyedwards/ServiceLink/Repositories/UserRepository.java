package com.wesleyedwards.ServiceLink.Repositories;

import com.wesleyedwards.ServiceLink.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByCredentialsUsername(String username);
}
