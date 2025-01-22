package com.lashana.backend.controller;

import com.lashana.backend.model.Diagnosis;
import com.lashana.backend.service.DiagnosisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/diagnosis")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final RestTemplate restTemplate;

    @Value("${deep.learning.api.url}")
    private String deepLearningApiUrl;

    public DiagnosisController(DiagnosisService diagnosisService, RestTemplate restTemplate) {
        this.diagnosisService = diagnosisService;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(deepLearningApiUrl + "/predict", requestEntity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null || !(boolean) responseBody.get("success")) {
                return ResponseEntity.status(500).body("Prediction failed from FastAPI");
            }

            String predictedLabel = (String) responseBody.get("predicted_label");
            double confidence = Double.parseDouble(responseBody.get("confidence").toString());

            Diagnosis diagnosis = new Diagnosis();
            diagnosis.setImagePath("uploaded/image/path");
            diagnosis.setPredictedLabel(predictedLabel);
            diagnosis.setConfidence(confidence);
            diagnosisService.saveDiagnosis(diagnosis);

            return ResponseEntity.ok(diagnosis);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing request: " + e.getMessage());
        }
    }
}
