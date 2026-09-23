package com.example.pdfgen.service;

import com.example.pdfgen.model.User;
import com.example.pdfgen.model.UserSession;
import com.example.pdfgen.repository.UserRepository;
import com.example.pdfgen.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles phone-number-based login and session management.
 *
 * <p>A user must already exist in the {@code users} table (pre-registered by an admin).
 * On a successful login a {@link UserSession} row is created with a UUID token
 * that the client stores and sends as a Bearer token on subsequent requests.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Sessions expire after 24 hours. */
    private static final long SESSION_HOURS = 24;

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;

    public AuthService(UserRepository userRepository, UserSessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Looks up a user by phone number and creates a new session token.
     *
     * @param phone 10-digit phone number
     * @return a UUID session token (as string) on success
     * @throws UserNotFoundException if the phone number is not registered
     */
    @Transactional
    public String login(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UserNotFoundException(
                        "Phone number " + phone + " is not registered."));

        UserSession session = new UserSession();
        session.setUser(user);
        session.setToken(UUID.randomUUID());
        session.setCreatedAt(OffsetDateTime.now());
        session.setExpiresAt(OffsetDateTime.now().plusHours(SESSION_HOURS));
        sessionRepository.save(session);

        log.info("Session created for user id={} phone={}", user.getId(), phone);
        return session.getToken().toString();
    }

    /**
     * Validates a Bearer token and returns the associated user if valid.
     *
     * @param token UUID string from the Authorization header
     * @return the User, or empty if the token is invalid/expired
     */
    @Transactional(readOnly = true)
    public Optional<User> validateToken(String token) {
        try {
            UUID uuid = UUID.fromString(token);
            return sessionRepository.findByToken(uuid)
                    .filter(s -> s.getExpiresAt() == null || s.getExpiresAt().isAfter(OffsetDateTime.now()))
                    .map(UserSession::getUser);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Thrown when the phone number is not in the users table. */
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String msg) { super(msg); }
    }
}
