package com.lashana.backend.service;

import com.lashana.backend.model.Diagnosis;
import com.lashana.backend.repository.DiagnosisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class DiagnosisService {
    private final DiagnosisRepository diagnosisRepository;
    private final Path fileStorageLocation;

    public DiagnosisService(DiagnosisRepository diagnosisRepository, 
                             @Value("${file.upload.path:uploads}") String uploadPath) {
        this.diagnosisRepository = diagnosisRepository;
        
        // Initialize fileStorageLocation
        this.fileStorageLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    public Diagnosis saveDiagnosis(Diagnosis diagnosis) {
        return diagnosisRepository.save(diagnosis);
    }

    public List<Diagnosis> getAllDiagnoses() {
        return diagnosisRepository.findAll();
    }

    public void deleteDiagnosis(Long id) {
        Diagnosis diagnosis = diagnosisRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Diagnosis not found"));
        
        // Xóa file ảnh
        try {
            Path filePath = fileStorageLocation.resolve(diagnosis.getImagePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete image file", e);
        }
        
        diagnosisRepository.delete(diagnosis);
    }
}