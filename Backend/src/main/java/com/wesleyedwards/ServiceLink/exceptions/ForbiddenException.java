package com.wesleyedwards.ServiceLink.exceptions;

/**
 * Thrown when an authenticated user is not permitted to access or act on a
 * resource (as opposed to not being authenticated at all). Mapped to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
