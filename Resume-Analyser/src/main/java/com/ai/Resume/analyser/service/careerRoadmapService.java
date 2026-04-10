package com.ai.Resume.analyser.service;

import com.ai.Resume.analyser.model.careerRoadmapRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class careerRoadmapService {
    @Value("${genKey}")
    private String genKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<?> generateRoadmap(careerRoadmapRequest req) {
        String prompt = "Create a practical 30-day career roadmap in strict JSON.\n" +
                "Target role: " + req.getTargetRole().trim() + "\n" +
                "Current skills: " + req.getCurrentSkills().trim() + "\n" +
                "Experience: " + req.getExperience().trim() + "\n\n" +
                "Rules:\n" +
                "1) Return only raw JSON.\n" +
                "2) Keep action items practical and coding-focused.\n" +
                "3) Suggest projects, DSA, system design, and interview prep.\n\n" +
                "JSON format:\n" +
                "{\n" +
                "  \"summary\": \"...\",\n" +
                "  \"skillGaps\": [\"...\"],\n" +
                "  \"weeklyPlan\": [\n" +
                "    {\"week\": \"Week 1\", \"focus\": \"...\", \"tasks\": [\"...\", \"...\"]}\n" +
                "  ],\n" +
                "  \"projectIdeas\": [\"...\"],\n" +
                "  \"interviewChecklist\": [\"...\"]\n" +
                "}";

        try {
            String raw = generateWithGrok(prompt);
            if (raw.startsWith("```")) {
                int firstBrace = raw.indexOf("{");
                int lastBrace = raw.lastIndexOf("}");
                if (firstBrace != -1 && lastBrace != -1) {
                    raw = raw.substring(firstBrace, lastBrace + 1);
                }
            }
            Map<?, ?> parsed = objectMapper.readValue(raw, Map.class);
            return new ResponseEntity<>(parsed, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("summary", "Unable to generate AI roadmap right now.");
            fallback.put("skillGaps", List.of("Retry after some time"));
            fallback.put("weeklyPlan", List.of());
            fallback.put("projectIdeas", List.of());
            fallback.put("interviewChecklist", List.of());
            return new ResponseEntity<>(fallback, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private String generateWithGrok(String prompt) throws IOException {
        if (genKey == null || genKey.isBlank()) {
            throw new IOException("Missing AI key");
        }
        String url;
        String model;

        if (genKey.startsWith("gsk_")) {
            url = "https://api.groq.com/openai/v1/chat/completions";
            model = "llama-3.3-70b-versatile";
        } else {
            url = "https://api.x.ai/v1/chat/completions";
            model = "grok-3-mini";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Objects.requireNonNull(genKey));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.3);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You generate structured JSON roadmaps only.");
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(systemMessage);
        messages.add(userMessage);
        body.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("AI response failed");
        }

        Map<?, ?> parsed = objectMapper.readValue(response.getBody(), Map.class);
        List<?> choices = (List<?>) parsed.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IOException("AI returned empty choices");
        }
        Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
        if (message == null || message.get("content") == null) {
            throw new IOException("AI returned empty content");
        }
        return message.get("content").toString();
    }
}
