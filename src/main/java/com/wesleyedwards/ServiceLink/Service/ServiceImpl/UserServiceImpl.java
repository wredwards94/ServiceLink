package com.wesleyedwards.ServiceLink.Service.ServiceImpl;

import com.wesleyedwards.ServiceLink.Entities.Credentials;
import com.wesleyedwards.ServiceLink.Entities.User;
import com.wesleyedwards.ServiceLink.Exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.Exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.Repositories.UserRepository;
import com.wesleyedwards.ServiceLink.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public User createUser(User newUser) {
//        User createdUser = new User();
//        Credentials credentials = new Credentials();
//        Profile profile = new Profile();
//
//        credentials.setUsername(newUser.getCredentials().getUsername());
//        credentials.setPassword(newUser.getCredentials().getPassword());
//
//        profile.setFirstName(newUser.getProfile().getFirstName());
//        profile.setLastName(newUser.getProfile().getLastName());
//        profile.setEmail(newUser.getProfile().getEmail());
//
//        createdUser.setCredentials(credentials);
//        createdUser.setProfile(profile);
//
//        userRepository.saveAndFlush(createdUser);
//
//        return createdUser;

        return userRepository.saveAndFlush(newUser);
    }

    @Override
    public User login(Credentials credentials) {
        Optional<User> optionalUser = userRepository.findByCredentialsUsername(credentials.getUsername());

        if(optionalUser.isEmpty()) throw new NotFoundException("user: " + credentials.getUsername() + " does not exist");
        if(!optionalUser.get().getCredentials().getPassword().equals(credentials.getPassword())) throw new BadRequestException("Invalid password");

        return optionalUser.get();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
