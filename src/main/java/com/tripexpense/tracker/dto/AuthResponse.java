package com.tripexpense.tracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String username;
    private String groupName;
    private Long groupId;
    private boolean hasGroup;
    private LocalDateTime createdAt;
    private int memberCount;
    
    @JsonProperty("isCreator")
    private boolean isCreator;
}
