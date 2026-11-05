package com.fitness.userservice.service;
import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.model.User;
import com.fitness.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository repository;

    // Upsert: creates the user, or (if the email exists) fills in any biometrics sent with the request.
    public UserResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            User existingUser = repository.findByEmail(request.getEmail());
            boolean changed = applyBiometrics(existingUser, request);
            if (changed) {
                existingUser = repository.save(existingUser);
            }
            return mapToResponse(existingUser);
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setKeycloakId(request.getKeycloakId());
        applyBiometrics(user, request);
        return mapToResponse(repository.save(user));
    }

    // Copies only the values that are present, so a token without the claims never wipes saved data.
    private boolean applyBiometrics(User user, RegisterRequest request) {
        boolean changed = false;
        if (request.getAge() != null && !request.getAge().equals(user.getAge())) {
            user.setAge(request.getAge());
            changed = true;
        }
        if (request.getWeight() != null && !request.getWeight().equals(user.getWeight())) {
            user.setWeight(request.getWeight());
            changed = true;
        }
        if (request.getHeight() != null && !request.getHeight().equals(user.getHeight())) {
            user.setHeight(request.getHeight());
            changed = true;
        }
        if (request.getGender() != null && !request.getGender().equals(user.getGender())) {
            user.setGender(request.getGender());
            changed = true;
        }
        return changed;
    }

    public UserResponse getUserProfile(String userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        return mapToResponse(user);
    }

    public Optional<UserResponse> getByKeycloakId(String keycloakId) {
        return repository.findByKeycloakId(keycloakId).map(this::mapToResponse);
    }

    public Boolean existByUserId(String userId) {
        return repository.existsByKeycloakId(userId);
    }

    private UserResponse mapToResponse(User user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setKeycloakId(user.getKeycloakId());
        r.setEmail(user.getEmail());
        r.setFirstName(user.getFirstName());
        r.setLastName(user.getLastName());
        r.setAge(user.getAge());
        r.setWeight(user.getWeight());
        r.setHeight(user.getHeight());
        r.setGender(user.getGender());
        r.setCreatedAt(user.getCreatedAt());
        r.setUpdatedId(user.getUpdatedId());
        return r;
    }
}
