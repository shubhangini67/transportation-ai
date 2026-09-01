package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.entity.Tenant;
import com.tms.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Multi-tenant management APIs")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all tenants")
    public ResponseEntity<ApiResponse<List<Tenant>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.getAllTenants()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get tenant by ID")
    public ResponseEntity<ApiResponse<Tenant>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.getTenantById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a tenant")
    public ResponseEntity<ApiResponse<Tenant>> create(@RequestBody Map<String, String> body) {
        Tenant tenant = tenantService.createTenant(body.get("name"), body.get("subdomain"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(tenant));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a tenant")
    public ResponseEntity<ApiResponse<Tenant>> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        String name = body.containsKey("name") ? (String) body.get("name") : null;
        String subdomain = body.containsKey("subdomain") ? (String) body.get("subdomain") : null;
        Boolean active = body.containsKey("active") ? (Boolean) body.get("active") : null;
        return ResponseEntity.ok(ApiResponse.ok(tenantService.updateTenant(id, name, subdomain, active)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a tenant")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }
}

