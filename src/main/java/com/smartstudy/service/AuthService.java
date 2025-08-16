package com.smartstudy.service;

import com.smartstudy.model.AppUser;
import com.smartstudy.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    public static final String SESSION_USER_ID = "USER_ID";

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public AppUser register(String username, String rawPassword, String name, String photoUrl) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setName(name);
        user.setPhotoUrl(photoUrl);
        return userRepository.save(user);
    }

    public Optional<AppUser> authenticate(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()));
    }

    public Map<String, Object> toPublicUser(AppUser u) {
        return Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "name", u.getName(),
                "photoUrl", u.getPhotoUrl()
        );
    }
}
