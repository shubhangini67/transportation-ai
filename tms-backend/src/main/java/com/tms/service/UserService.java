package com.tms.service;

import com.tms.dto.response.UserResponse;
import com.tms.entity.User;
import com.tms.enums.UserRole;
import com.tms.exception.BadRequestException;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Transactional
    public UserResponse changeRole(UUID userId, UserRole role, String currentUsername) {
        User user = findById(userId);
        if (user.getUsername().equals(currentUsername)) {
            throw new BadRequestException("Cannot change your own role");
        }
        user.setRole(role);
        userRepository.save(user);
        log.info("Role changed for user '{}' to {}", user.getUsername(), role);
        return toResponse(user);
    }

    @Transactional
    public UserResponse deactivateUser(UUID userId, String currentUsername) {
        User user = findById(userId);
        if (user.getUsername().equals(currentUsername)) {
            throw new BadRequestException("Cannot deactivate your own account");
        }
        user.setActive(false);
        userRepository.save(user);
        log.info("User '{}' deactivated", user.getUsername());
        return toResponse(user);
    }

    @Transactional
    public UserResponse activateUser(UUID userId) {
        User user = findById(userId);
        user.setActive(true);
        userRepository.save(user);
        log.info("User '{}' activated", user.getUsername());
        return toResponse(user);
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .role(u.getRole())
                .active(u.getActive())
                .avatarUrl(u.getAvatarUrl())
                .createdAt(u.getCreatedAt())
                .build();
    }
}

