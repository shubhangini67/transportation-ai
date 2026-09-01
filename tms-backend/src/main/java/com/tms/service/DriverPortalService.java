package com.tms.service;

import com.tms.dto.response.TripResponse;
import com.tms.entity.Driver;
import com.tms.entity.User;
import com.tms.enums.TripStatus;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.DriverRepository;
import com.tms.repository.TripRepository;
import com.tms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DriverPortalService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final TripService tripService;

    @Transactional(readOnly = true)
    public Map<String, Object> myBoard() {
        Driver driver = requireLinkedDriver();
        List<TripResponse> trips = tripRepository.findByDriverIdOrderByStartTimeDesc(driver.getId())
                .stream()
                .map(tripService::toResponse)
                .toList();

        Map<String, Object> board = new LinkedHashMap<>();
        board.put("driverId", driver.getId());
        board.put("driverName", driver.getName());
        board.put("trips", trips);
        board.put("plannedCount", trips.stream().filter(t -> t.getStatus() == TripStatus.PLANNED).count());
        board.put("inProgressCount", trips.stream().filter(t -> t.getStatus() == TripStatus.IN_PROGRESS).count());
        board.put("completedCount", trips.stream().filter(t -> t.getStatus() == TripStatus.COMPLETED).count());
        return board;
    }

    public Driver requireLinkedDriver() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return driverRepository.findByEmailIgnoreCase(user.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Driver profile", "email", user.getEmail() + " — ask admin to link your login email to a driver record"));
    }
}
