package com.tms.repository;

import com.tms.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByActiveTrue();
    List<Route> findByOriginContainingIgnoreCaseOrDestinationContainingIgnoreCase(String origin, String destination);
}

