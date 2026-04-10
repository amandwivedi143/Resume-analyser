package com.ai.Resume.analyser.controller;

import com.ai.Resume.analyser.model.codingTestRequest;
import com.ai.Resume.analyser.model.codingTestSubmission;
import com.ai.Resume.analyser.service.codingTestService;
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
@RequestMapping("resumeAnalyserCore/service/v1/codingTest")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.HEAD})
public class codingTestController {
    @Autowired
    private codingTestService codingTestService;

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@Valid @RequestBody codingTestRequest req) {
        return codingTestService.generateTest(req);
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@Valid @RequestBody codingTestSubmission req) {
        return codingTestService.submitTest(req);
    }
}
