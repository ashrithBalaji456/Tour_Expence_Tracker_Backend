package com.tripexpense.tracker.controller;

import com.tripexpense.tracker.dto.*;
import com.tripexpense.tracker.entity.PreTripMember;
import com.tripexpense.tracker.entity.TripGroup;
import com.tripexpense.tracker.entity.User;
import com.tripexpense.tracker.repository.PreTripMemberRepository;
import com.tripexpense.tracker.repository.TripGroupRepository;
import com.tripexpense.tracker.repository.UserRepository;
import com.tripexpense.tracker.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final TripGroupRepository tripGroupRepository;
    private final PreTripMemberRepository preTripMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists!");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail().trim())
                .build();

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername().trim());
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password!");
        }

        User user = userOpt.get();
        String token = jwtTokenProvider.generateToken(user.getUsername());

        // Find active group for user
        List<TripGroup> groups = tripGroupRepository.findAssociatedGroups(user.getUsername(), user.getEmail());
        
        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
                .token(token)
                .username(user.getUsername());

        if (!groups.isEmpty()) {
            TripGroup activeGroup = groups.get(0); // Take the first active group
            builder.hasGroup(true)
                    .groupId(activeGroup.getId())
                    .groupName(activeGroup.getName())
                    .isCreator(activeGroup.getCreator().getUsername().equalsIgnoreCase(user.getUsername()));
        } else {
            builder.hasGroup(false);
        }

        return ResponseEntity.ok(builder.build());
    }

    @PostMapping("/group")
    public ResponseEntity<?> createGroup(@RequestBody GroupRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> creatorOpt = userRepository.findByUsername(currentUsername);
        
        if (creatorOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        User creator = creatorOpt.get();
        
        // Build unique member list (ensure creator is not added manually but registered, and remove duplicates)
        List<String> members = new ArrayList<>();
        if (request.getMemberUsernames() != null) {
            for (String rawName : request.getMemberUsernames()) {
                String name = rawName.trim();
                if (name.isBlank() || name.equalsIgnoreCase(creator.getUsername())) {
                    continue;
                }
                
                // Try to resolve username from either raw username or raw email
                Optional<User> invitedUserOpt = userRepository.findByUsernameOrEmail(name, name);
                if (invitedUserOpt.isPresent()) {
                    String resolvedUsername = invitedUserOpt.get().getUsername();
                    if (!resolvedUsername.equalsIgnoreCase(creator.getUsername()) && !members.contains(resolvedUsername)) {
                        members.add(resolvedUsername);
                    }
                } else {
                    // Storing raw name/email as fallback if user has not registered yet
                    if (!members.contains(name)) {
                        members.add(name);
                    }
                }
            }
        }

        TripGroup group = TripGroup.builder()
                .name(request.getGroupName().trim())
                .creator(creator)
                .memberUsernames(members)
                .createdAt(LocalDateTime.now())
                .build();

        TripGroup savedGroup = tripGroupRepository.save(group);

        // Auto-create PreTripMember for the creator
        PreTripMember creatorMember = PreTripMember.builder()
                .name(creator.getUsername())
                .budgetLimit(new BigDecimal("10000.00"))
                .tripGroup(savedGroup)
                .build();
        preTripMemberRepository.save(creatorMember);

        // Auto-create PreTripMembers for the other invited members
        for (String mName : members) {
            PreTripMember member = PreTripMember.builder()
                    .name(mName)
                    .budgetLimit(new BigDecimal("10000.00"))
                    .tripGroup(savedGroup)
                    .build();
            preTripMemberRepository.save(member);
        }

        return ResponseEntity.ok(AuthResponse.builder()
                .hasGroup(true)
                .groupId(savedGroup.getId())
                .groupName(savedGroup.getName())
                .isCreator(true)
                .username(creator.getUsername())
                .build());
    }

    @GetMapping("/groups")
    public ResponseEntity<?> getMyGroups() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userRepository.findByUsername(currentUsername);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        User user = userOpt.get();
        List<TripGroup> groups = tripGroupRepository.findAssociatedGroups(currentUsername, user.getEmail());

        List<AuthResponse> dtos = groups.stream().map(g -> {
            boolean isCreator = g.getCreator().getUsername().equalsIgnoreCase(currentUsername);
            return AuthResponse.builder()
                    .groupId(g.getId())
                    .groupName(g.getName())
                    .username(currentUsername)
                    .hasGroup(true)
                    .isCreator(isCreator)
                    .build();
        }).toList();

        return ResponseEntity.ok(dtos);
    }
}
