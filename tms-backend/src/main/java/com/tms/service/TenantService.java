package com.tms.service;

import com.tms.entity.Tenant;
import com.tms.exception.DuplicateResourceException;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Tenant getTenantById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", id));
    }

    @Transactional
    public Tenant createTenant(String name, String subdomain) {
        if (tenantRepository.existsBySubdomain(subdomain)) {
            throw new DuplicateResourceException("Tenant", "subdomain", subdomain);
        }
        Tenant tenant = Tenant.builder()
                .name(name)
                .subdomain(subdomain)
                .active(true)
                .build();
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant created: {} ({})", saved.getName(), saved.getId());
        return saved;
    }

    @Transactional
    public Tenant updateTenant(UUID id, String name, String subdomain, Boolean active) {
        Tenant tenant = getTenantById(id);
        if (name != null) tenant.setName(name);
        if (subdomain != null) {
            tenantRepository.findBySubdomain(subdomain)
                    .filter(t -> !t.getId().equals(id))
                    .ifPresent(t -> {
                        throw new DuplicateResourceException("Tenant", "subdomain", subdomain);
                    });
            tenant.setSubdomain(subdomain);
        }
        if (active != null) tenant.setActive(active);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = getTenantById(id);
        tenantRepository.delete(tenant);
        log.info("Tenant deleted: {}", id);
    }
}

