package com.atlas.atlas_backend.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final int MAX_ATTEMPT = 5;
    private final int BLOCK_DURATION_MINUTES = 1;

    private final Map<String, LoginAttempt> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
    }

    public void loginFailed(String key) {
        LoginAttempt attempt = attemptsCache.getOrDefault(key, new LoginAttempt(0, LocalDateTime.now()));
        attempt.setAttempts(attempt.getAttempts() + 1);
        attempt.setLastModified(LocalDateTime.now());
        attemptsCache.put(key, attempt);
    }

    public boolean isBlocked(String key) {
        LoginAttempt attempt = attemptsCache.get(key);
        if (attempt != null) {
            if (attempt.getAttempts() >= MAX_ATTEMPT) {
                if (attempt.getLastModified().plusMinutes(BLOCK_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
                    return true;
                } else {
                    // Unblock after duration
                    attemptsCache.remove(key);
                }
            }
        }
        return false;
    }

    @Scheduled(fixedRate = 60000)
    public void cleanUp() {
        LocalDateTime now = LocalDateTime.now();
        attemptsCache.entrySet().removeIf(entry ->
                entry.getValue().getLastModified().plusMinutes(BLOCK_DURATION_MINUTES).isBefore(now)
        );
    }

    private static class LoginAttempt {
        private int attempts;
        private LocalDateTime lastModified;

        public LoginAttempt(int attempts, LocalDateTime lastModified) {
            this.attempts = attempts;
            this.lastModified = lastModified;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        public LocalDateTime getLastModified() {
            return lastModified;
        }

        public void setLastModified(LocalDateTime lastModified) {
            this.lastModified = lastModified;
        }
    }
}
