package com.ai.Resume.analyser.service;

import com.ai.Resume.analyser.model.codingTestRequest;
import com.ai.Resume.analyser.model.codingTestSubmission;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class codingTestService {
    @Value("${genKey}")
    private String genKey;

    private final ConcurrentHashMap<String, StoredTest> activeTests = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<?> generateTest(codingTestRequest req) {
        cleanupExpiredTests();

        String prompt = buildPrompt(req);
        String generatedJson;
        try {
            generatedJson = generateWithGrok(prompt);
            if (generatedJson.startsWith("```")) {
                int firstBrace = generatedJson.indexOf("{");
                int lastBrace = generatedJson.lastIndexOf("}");
                if (firstBrace != -1 && lastBrace != -1) {
                    generatedJson = generatedJson.substring(firstBrace, lastBrace + 1);
                }
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Unable to generate coding test right now", HttpStatus.SERVICE_UNAVAILABLE);
        }

        GrokTestPayload payload;
        try {
            payload = objectMapper.readValue(generatedJson, GrokTestPayload.class);
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid AI response while generating test", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (payload.questions == null || payload.questions.isEmpty()) {
            return new ResponseEntity<>("No questions generated", HttpStatus.SERVICE_UNAVAILABLE);
        }

        String sanitizedDifficulty = sanitizeDifficulty(req.getDifficulty());
        int durationSeconds = calculateDurationSeconds(sanitizedDifficulty, req.getNumberOfQuestions());
        String testId = UUID.randomUUID().toString();
        long expireAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        activeTests.put(testId, new StoredTest(payload.questions, expireAt, req.getRole().trim(), req.getExperience().trim(), req.getTopic().trim(), sanitizedDifficulty));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("testId", testId);
        response.put("role", req.getRole().trim());
        response.put("experience", req.getExperience().trim());
        response.put("topic", req.getTopic().trim());
        response.put("difficulty", sanitizedDifficulty);
        response.put("durationSeconds", durationSeconds);

        List<Map<String, Object>> safeQuestions = new ArrayList<>();
        for (GrokQuestion q : payload.questions) {
            if (q == null || q.id == null || q.question == null || q.options == null || q.options.size() < 2) {
                continue;
            }
            Map<String, Object> safeQuestion = new LinkedHashMap<>();
            safeQuestion.put("id", q.id);
            safeQuestion.put("question", q.question);
            safeQuestion.put("options", q.options);
            safeQuestion.put("focusArea", q.focusArea == null || q.focusArea.isBlank() ? req.getTopic().trim() : q.focusArea.trim());
            safeQuestions.add(safeQuestion);
        }
        response.put("questions", safeQuestions);

        if (safeQuestions.isEmpty()) {
            activeTests.remove(testId);
            return new ResponseEntity<>("Unable to prepare test questions", HttpStatus.SERVICE_UNAVAILABLE);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<?> submitTest(codingTestSubmission req) {
        cleanupExpiredTests();

        StoredTest stored = activeTests.get(req.getTestId());
        if (stored == null || stored.expireAt < System.currentTimeMillis()) {
            activeTests.remove(req.getTestId());
            return new ResponseEntity<>("Test expired or invalid test id", HttpStatus.NOT_ACCEPTABLE);
        }

        int total = stored.questions.size();
        int attempted = 0;
        int correct = 0;
        List<Map<String, Object>> review = new ArrayList<>();
        Set<String> weakAreas = new LinkedHashSet<>();

        for (GrokQuestion q : stored.questions) {
            Integer selected = req.getAnswers().get(q.id);
            if (selected == null) {
                selected = 0;
            } else {
                attempted++;
            }
            boolean isCorrect = selected == q.correctOption;
            if (isCorrect) {
                correct++;
            } else {
                weakAreas.add(resolveFocusArea(q, stored.topic));
            }

            Map<String, Object> reviewItem = new LinkedHashMap<>();
            reviewItem.put("id", q.id);
            reviewItem.put("question", q.question);
            reviewItem.put("selectedOption", selected);
            reviewItem.put("correctOption", q.correctOption);
            reviewItem.put("isCorrect", isCorrect);
            reviewItem.put("focusArea", resolveFocusArea(q, stored.topic));
            reviewItem.put("explanation", q.explanation == null ? "Review this concept and try similar coding questions." : q.explanation);
            review.add(reviewItem);
        }

        double percentage = total == 0 ? 0 : (correct * 100.0) / total;
        String feedback;
        if (percentage >= 85) {
            feedback = "Excellent performance. You are interview ready for this topic.";
        } else if (percentage >= 60) {
            feedback = "Good attempt. Revise weak areas and retry for better score.";
        } else {
            feedback = "Needs improvement. Practice fundamentals and solve more problems.";
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalQuestions", total);
        response.put("attempted", attempted);
        response.put("correct", correct);
        response.put("scorePercentage", Math.round(percentage));
        response.put("feedback", feedback);
        response.put("review", review);
        response.put("weakAreas", new ArrayList<>(weakAreas));
        response.put("recommendations", buildRecommendations(percentage, weakAreas));

        Map<String, Object> retryPayload = new LinkedHashMap<>();
        retryPayload.put("role", stored.role);
        retryPayload.put("experience", stored.experience);
        retryPayload.put("topic", weakAreas.isEmpty() ? stored.topic : String.join(", ", weakAreas));
        retryPayload.put("difficulty", stored.difficulty);
        retryPayload.put("numberOfQuestions", Math.max(3, Math.min(8, total)));
        response.put("retryPayload", retryPayload);

        activeTests.remove(req.getTestId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void cleanupExpiredTests() {
        long now = System.currentTimeMillis();
        activeTests.entrySet().removeIf(entry -> entry.getValue().expireAt < now);
    }

    private String buildPrompt(codingTestRequest req) {
        String difficulty = sanitizeDifficulty(req.getDifficulty());
        return "Generate a role-based coding test in strict JSON.\n" +
                "Role: " + req.getRole().trim() + "\n" +
                "Experience: " + req.getExperience().trim() + "\n" +
                "Topic: " + req.getTopic().trim() + "\n" +
                "Difficulty: " + difficulty + "\n" +
                "Number of questions: " + req.getNumberOfQuestions() + "\n\n" +
                "Rules:\n" +
                "1) Return ONLY raw JSON, no markdown.\n" +
                "2) Questions must be practical and interview-level.\n" +
                "3) Each question must have 4 options.\n" +
                "4) correctOption must be 1, 2, 3, or 4.\n" +
                "5) Use concise clear wording.\n" +
                "6) Adjust complexity to the given difficulty.\n" +
                "7) Include focusArea for each question.\n\n" +
                "JSON schema:\n" +
                "{\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"id\": \"q1\",\n" +
                "      \"question\": \"...\",\n" +
                "      \"options\": [\"...\", \"...\", \"...\", \"...\"],\n" +
                "      \"correctOption\": 1,\n" +
                "      \"explanation\": \"short reason\",\n" +
                "      \"focusArea\": \"specific sub-topic\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private String resolveFocusArea(GrokQuestion q, String fallbackTopic) {
        if (q.focusArea == null || q.focusArea.isBlank()) {
            return fallbackTopic;
        }
        return q.focusArea.trim();
    }

    private List<String> buildRecommendations(double percentage, Set<String> weakAreas) {
        List<String> recommendations = new ArrayList<>();
        if (percentage < 50) {
            recommendations.add("Revise core concepts first and retake with medium difficulty.");
            recommendations.add("Solve 10-15 focused coding problems per weak area.");
        } else if (percentage < 80) {
            recommendations.add("Practice timed coding to improve speed and accuracy.");
            recommendations.add("Reattempt weak areas with hard follow-up questions.");
        } else {
            recommendations.add("Great performance. Attempt hard difficulty mock interview rounds.");
            recommendations.add("Keep consistency with one mixed-topic test daily.");
        }
        if (!weakAreas.isEmpty()) {
            recommendations.add("Priority weak areas: " + String.join(", ", weakAreas));
        }
        return recommendations;
    }

    private String sanitizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return "medium";
        }
        String normalized = difficulty.trim().toLowerCase();
        if (!normalized.equals("easy") && !normalized.equals("medium") && !normalized.equals("hard")) {
            return "medium";
        }
        return normalized;
    }

    private int calculateDurationSeconds(String difficulty, int totalQuestions) {
        int basePerQuestion;
        switch (difficulty) {
            case "easy" -> basePerQuestion = 75;
            case "hard" -> basePerQuestion = 150;
            default -> basePerQuestion = 110;
        }
        return Math.max(180, totalQuestions * basePerQuestion);
    }

    private String generateWithGrok(String prompt) throws IOException {
        if (genKey == null || genKey.isBlank()) {
            throw new IOException("Missing AI key");
        }
        String url;
        String model;

        if (genKey != null && genKey.startsWith("gsk_")) {
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
        systemMessage.put("content", "You generate coding tests and return valid JSON only.");
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(systemMessage);
        messages.add(userMessage);
        body.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Grok response failed");
        }

        Map<?, ?> parsed = objectMapper.readValue(response.getBody(), Map.class);
        List<?> choices = (List<?>) parsed.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IOException("Grok returned empty choices");
        }
        Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
        if (message == null || message.get("content") == null) {
            throw new IOException("Grok returned empty message content");
        }
        return message.get("content").toString();
    }

    private record StoredTest(List<GrokQuestion> questions, long expireAt, String role, String experience, String topic, String difficulty) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GrokTestPayload {
        @JsonProperty("questions")
        public List<GrokQuestion> questions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GrokQuestion {
        @JsonProperty("id")
        public String id;

        @JsonProperty("question")
        public String question;

        @JsonProperty("options")
        public List<String> options;

        @JsonProperty("correctOption")
        public int correctOption;

        @JsonProperty("explanation")
        public String explanation;

        @JsonProperty("focusArea")
        public String focusArea;
    }
}
