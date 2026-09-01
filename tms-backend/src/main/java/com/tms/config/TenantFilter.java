package com.tms.config;

import com.tms.repository.TenantRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that extracts the X-Tenant-ID header,
 * validates the tenant exists and is active, then sets TenantContext.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class TenantFilter implements Filter {

    private final TenantRepository tenantRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        String tenantHeader = httpReq.getHeader("X-Tenant-ID");

        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                UUID tenantId = UUID.fromString(tenantHeader);
                boolean valid = tenantRepository.existsByIdAndActiveTrue(tenantId);
                if (!valid) {
                    log.warn("Invalid or inactive tenant: {}", tenantHeader);
                    httpRes.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    httpRes.getWriter().write("{\"error\":\"Invalid or inactive tenant\"}");
                    return;
                }
                TenantContext.setCurrentTenant(tenantHeader);
            } catch (IllegalArgumentException e) {
                log.warn("Malformed tenant ID: {}", tenantHeader);
                httpRes.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpRes.getWriter().write("{\"error\":\"Malformed tenant ID\"}");
                return;
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

