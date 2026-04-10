package com.ai.Resume.analyser.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class codingTestSubmission {
    @NotBlank(message = "Test id is required")
    private String testId;

    @NotNull(message = "Answers are required")
    private Map<String, Integer> answers;
}
