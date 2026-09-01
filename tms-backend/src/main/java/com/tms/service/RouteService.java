package com.tms.service;

import com.tms.dto.request.RouteRequest;
import com.tms.dto.response.RouteResponse;
import com.tms.entity.Route;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteService {

    private final RouteRepository routeRepository;

    @Transactional(readOnly = true)
    public Page<RouteResponse> getAllRoutes(int page, int size) {
        log.debug("Fetching routes page={} size={}", page, size);
        return routeRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RouteResponse getRouteById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> getActiveRoutes() {
        return routeRepository.findByActiveTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> searchRoutes(String query) {
        return routeRepository.findByOriginContainingIgnoreCaseOrDestinationContainingIgnoreCase(query, query)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public RouteResponse createRoute(RouteRequest request) {
        Route route = Route.builder()
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .distance(request.getDistance())
                .estimatedTimeMinutes(request.getEstimatedTimeMinutes())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        return toResponse(routeRepository.save(route));
    }

    @Transactional
    public RouteResponse updateRoute(Long id, RouteRequest request) {
        Route route = findById(id);
        route.setOrigin(request.getOrigin());
        route.setDestination(request.getDestination());
        route.setDistance(request.getDistance());
        route.setEstimatedTimeMinutes(request.getEstimatedTimeMinutes());
        route.setDescription(request.getDescription());
        if (request.getActive() != null) route.setActive(request.getActive());
        return toResponse(routeRepository.save(route));
    }

    @Transactional
    public void deleteRoute(Long id) {
        Route route = findById(id);
        routeRepository.delete(route);
    }

    Route findById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", id));
    }

    private RouteResponse toResponse(Route r) {
        return RouteResponse.builder()
                .id(r.getId())
                .origin(r.getOrigin())
                .destination(r.getDestination())
                .distance(r.getDistance())
                .estimatedTimeMinutes(r.getEstimatedTimeMinutes())
                .description(r.getDescription())
                .active(r.getActive())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

