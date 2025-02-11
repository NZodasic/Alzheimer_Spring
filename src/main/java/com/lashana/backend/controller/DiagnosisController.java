package com.lashana.backend.controller;

import com.lashana.backend.model.Diagnosis;
import com.lashana.backend.service.DiagnosisService;
import com.lashana.backend.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;

@RestController
@RequestMapping("/api/diagnosis")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final RestTemplate restTemplate;
    private final ActivityLogService activityLogService;
    private Path fileStorageLocation;

    @Value("${deep.learning.api.url}")
    private String deepLearningApiUrl;

    @Value("${file.upload.path:uploads}")  // Default value if property is not set
    private String uploadPath;

    public DiagnosisController(DiagnosisService diagnosisService, RestTemplate restTemplate, ActivityLogService activityLogService) {
        this.diagnosisService = diagnosisService;
        this.restTemplate = restTemplate;
        this.activityLogService = activityLogService;
    }

    @PostConstruct
    public void init() {
        try {
            this.fileStorageLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory!", ex);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            String username = authentication.getName();
            activityLogService.logActivity(username, "UPLOAD_IMAGE");
    
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }
    
            // Lưu ảnh vào hệ thống
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path targetLocation = fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation);
    
            // ✅ Gửi ảnh tới FastAPI để predict
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
    
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                deepLearningApiUrl + "/predict",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
    
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !(boolean) responseBody.get("success")) {
                return ResponseEntity.status(500).body("Prediction failed from FastAPI");
            }
    
            // Lưu kết quả predict vào database
            String predictedLabel = (String) responseBody.get("predicted_label");
            double confidence = Double.parseDouble(responseBody.get("confidence").toString());
    
            Diagnosis diagnosis = new Diagnosis();
            diagnosis.setImagePath(fileName);
            diagnosis.setPredictedLabel(predictedLabel);
            diagnosis.setConfidence(confidence);
            diagnosisService.saveDiagnosis(diagnosis);
    
            return ResponseEntity.ok(diagnosis);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing request: " + e.getMessage());
        }
    }
    

    @GetMapping("/all")
    public ResponseEntity<List<Diagnosis>> getAllDiagnoses() {
        List<Diagnosis> diagnoses = diagnosisService.getAllDiagnoses();
        return ResponseEntity.ok(diagnoses);
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDiagnosis(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role -> role.equals("ADMIN"));

        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only Admins can delete images.");
        }

        String username = authentication.getName();
        activityLogService.logActivity(username, "DELETE_IMAGE"); // Ghi nhật ký

        try {
            diagnosisService.deleteDiagnosis(id);
            return ResponseEntity.ok("Diagnosis deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
