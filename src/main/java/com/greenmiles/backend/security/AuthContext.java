package com.greenmiles.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthContext {

    public boolean isAdmin() {
        return "ADMIN".equals(currentRole());
    }

    public String currentRole() {
        String principal = currentPrincipal();
        if (principal == null || !principal.contains(":")) {
            throw new AccessDeniedException("Invalid authentication context");
        }
        return principal.split(":", 2)[0];
    }

    public Long currentUserId() {
        if (!"USER".equals(currentRole())) {
            throw new AccessDeniedException("User token required");
        }
        return Long.parseLong(currentPrincipal().split(":", 2)[1]);
    }

    public Long currentDriverId() {
        if (!"DRIVER".equals(currentRole())) {
            throw new AccessDeniedException("Driver token required");
        }
        return Long.parseLong(currentPrincipal().split(":", 2)[1]);
    }

    public Long currentActorIdOrNull() {
        try {
            String principal = currentPrincipal();
            String[] parts = principal.split(":", 2);
            if (parts.length == 2 && ("USER".equals(parts[0]) || "DRIVER".equals(parts[0]))) {
                return Long.parseLong(parts[1]);
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Unauthorized");
        }
        return authentication.getName();
    }
}
