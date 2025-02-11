package com.lashana.backend.service;

import com.lashana.backend.model.ActivityLog;
import com.lashana.backend.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void logActivity(String username, String action) {
        ActivityLog log = new ActivityLog();
        log.setUsername(username);
        log.setAction(action);
        activityLogRepository.save(log);
    }

    public List<ActivityLog> getAllLogs() {
        return activityLogRepository.findAll();
    }
    public void deleteLog(Long id) {
        activityLogRepository.deleteById(id);
    }
    
    public void deleteAllLogs() {
        activityLogRepository.deleteAll();
    }
    
}
