package com.ai.Resume.analyser.controller;

import com.ai.Resume.analyser.model.careerRoadmapRequest;
import com.ai.Resume.analyser.service.careerRoadmapService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("resumeAnalyserCore/service/v1/career")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.HEAD})
public class careerRoadmapController {
    @Autowired
    private careerRoadmapService careerRoadmapService;

    @PostMapping("/roadmap")
    public ResponseEntity<?> roadmap(@Valid @RequestBody careerRoadmapRequest req) {
        return careerRoadmapService.generateRoadmap(req);
    }
}
