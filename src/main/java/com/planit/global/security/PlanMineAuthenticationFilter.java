package com.planit.global.security;

import com.planit.global.common.exception.UnauthorizedAccessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class PlanMineAuthenticationFilter extends OncePerRequestFilter {

    private static final String PLAN_PATH = "/api/plans";
    private static final String MINE_QUERY = "mine";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        if (!PLAN_PATH.equals(request.getRequestURI())) {
            return true;
        }
        String rawMine = request.getParameter(MINE_QUERY);
        if (!"true".equalsIgnoreCase(rawMine)) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isAuthenticated()) {
            log.debug("Plan mine endpoint triggered without authentication (uri={}, query={})",
                request.getRequestURI(), request.getQueryString());
            throw new UnauthorizedAccessException();
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return !(authentication instanceof AnonymousAuthenticationToken);
    }
}
