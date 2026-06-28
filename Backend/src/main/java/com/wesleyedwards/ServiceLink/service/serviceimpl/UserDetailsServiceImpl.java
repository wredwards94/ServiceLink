package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByCredentialsUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User: " + username + " not found"));

        return new UserPrincipal(user);
    }
}
