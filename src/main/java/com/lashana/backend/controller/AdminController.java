package com.lashana.backend.controller;

import com.lashana.backend.model.ActivityLog;
import com.lashana.backend.model.User;
import com.lashana.backend.service.UserService;
import com.lashana.backend.service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;


import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserService userService;
    private final ActivityLogService activityLogService;
    
    public AdminController(UserService userService, ActivityLogService activityLogService) {
        this.userService = userService;
        this.activityLogService = activityLogService; // Khởi tạo ActivityLogService
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/set-admin/{userId}")
    public ResponseEntity<?> makeAdmin(@PathVariable Long userId) {
        try {
            userService.setUserRole(userId, "ADMIN");
            return ResponseEntity.ok("User promoted to admin");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/logs/{id}")
    public ResponseEntity<?> deleteLog(@PathVariable Long id) {
        try {
            activityLogService.deleteLog(id);
            return ResponseEntity.ok("Log deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting log: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/logs")
    public ResponseEntity<?> deleteAllLogs() {
        try {
            activityLogService.deleteAllLogs();
            return ResponseEntity.ok("All logs deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting all logs: " + e.getMessage());
        }
    }
    @GetMapping("/logs")
    public ResponseEntity<List<ActivityLog>> getActivityLogs() {
        return ResponseEntity.ok(activityLogService.getAllLogs());
    }
    
}
