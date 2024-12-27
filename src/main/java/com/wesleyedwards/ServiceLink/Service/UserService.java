package com.wesleyedwards.ServiceLink.Service;

import com.wesleyedwards.ServiceLink.Entities.Credentials;
import com.wesleyedwards.ServiceLink.Entities.User;

public interface UserService {
    User createUser(User newUser);

    User login(Credentials credentials);
}
