package com.resumeanalyzer.api.security;

import com.resumeanalyzer.api.entity.User;
import com.resumeanalyzer.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        log.info("OAuth2 login attempt for email: {}", email);

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existingUser -> linkGoogleAccount(existingUser, googleId, picture))
                        .orElseGet(() -> createNewGoogleUser(googleId, email, name, picture)));

        log.info("OAuth2 login successful for user: {}", user.getEmail());

        return oAuth2User;
    }

    private User linkGoogleAccount(User user, String googleId, String picture) {
        log.info("Linking Google account to existing user: {}", user.getEmail());
        user.setGoogleId(googleId);
        user.setProfilePicture(picture);
        user.setAuthProvider(User.AuthProvider.GOOGLE);
        return userRepository.save(user);
    }

    private User createNewGoogleUser(
            String googleId, String email, String name, String picture) {

        log.info("Creating new user from Google OAuth2: {}", email);

        String username = generateUniqueUsername(name, email);

        User newUser = User.builder()
                .googleId(googleId)
                .email(email)
                .username(username)
                .profilePicture(picture)
                .authProvider(User.AuthProvider.GOOGLE)
                .role(User.Role.USER)
                .enabled(true)
                .build();

        return userRepository.save(newUser);
    }

    private String generateUniqueUsername(String name, String email) {
        String baseUsername = name != null
                ? name.toLowerCase().replaceAll("[^a-z0-9]", "")
                : email.split("@")[0].toLowerCase();

        if (baseUsername.length() < 3) {
            baseUsername = "user" + baseUsername;
        }

        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}