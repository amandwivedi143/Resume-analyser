package com.ai.Resume.analyser.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class codingTestRequest {
    @NotBlank(message = "Role is required")
    private String role;

    @NotBlank(message = "Experience is required")
    private String experience;

    @NotBlank(message = "Topic is required")
    private String topic;

    @Pattern(regexp = "easy|medium|hard", message = "Difficulty must be easy, medium, or hard")
    private String difficulty = "medium";

    @Min(value = 3, message = "Minimum 3 questions required")
    @Max(value = 15, message = "Maximum 15 questions allowed")
    private int numberOfQuestions = 5;
}
