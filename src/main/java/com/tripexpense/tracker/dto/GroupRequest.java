package com.tripexpense.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {
    private String groupName;
    private List<String> memberUsernames;
}
