package com.ai.Resume.analyser.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class careerRoadmapRequest {
    @NotBlank(message = "Target role is required")
    private String targetRole;

    @NotBlank(message = "Current skills are required")
    private String currentSkills;

    @NotBlank(message = "Experience is required")
    private String experience;
}
