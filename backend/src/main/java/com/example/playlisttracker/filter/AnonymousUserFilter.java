package com.example.playlisttracker.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that runs on every request to ensure the caller has an
 * anonymous identity cookie.  The anonymous user ID is read exclusively from
 * the server-set HttpOnly cookie – it is NEVER trusted from query params or
 * the request body.
 *
 * <p>The resolved ID is stored in a request attribute so that downstream
 * controllers and services can retrieve it safely.
 */
@Component
@Order(1)
public class AnonymousUserFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AnonymousUserFilter.class);
    public static final String ANONYMOUS_USER_ID_ATTRIBUTE = "anonymousUserId";
    public static final String COOKIE_NAME = "auid";

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    @Value("${app.cookie.max-age-seconds:31536000}")
    private int cookieMaxAge;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String anonymousUserId = extractAnonymousUserIdFromCookie(request);

        if (anonymousUserId == null || anonymousUserId.isBlank()) {
            anonymousUserId = UUID.randomUUID().toString();
            log.debug("New anonymous user created: {}", anonymousUserId);
            writeAnonymousUserCookie(response, anonymousUserId);
        } else {
            log.debug("Existing anonymous user identified: {}", anonymousUserId);
        }

        // Store in request attribute — controllers read from here, never from the request body
        request.setAttribute(ANONYMOUS_USER_ID_ATTRIBUTE, anonymousUserId);

        filterChain.doFilter(request, response);
    }

    private String extractAnonymousUserIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                // Basic validation — must look like a UUID
                if (value != null && value.matches("[0-9a-fA-F\\-]{36}")) {
                    return value;
                }
                log.warn("Invalid anonymous user cookie value received, ignoring");
                return null;
            }
        }
        return null;
    }

    private void writeAnonymousUserCookie(HttpServletResponse response, String anonymousUserId) {
        // Build Set-Cookie header manually to support SameSite attribute
        StringBuilder header = new StringBuilder();
        header.append(COOKIE_NAME).append("=").append(anonymousUserId);
        header.append("; Max-Age=").append(cookieMaxAge);
        header.append("; Path=/");
        header.append("; HttpOnly");
        header.append("; SameSite=").append(cookieSameSite);

        if (cookieSecure) {
            header.append("; Secure");
        }

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            header.append("; Domain=").append(cookieDomain);
        }

        response.addHeader("Set-Cookie", header.toString());
        log.debug("Anonymous user cookie written");
    }
}
