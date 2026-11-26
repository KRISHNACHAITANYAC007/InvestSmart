package com.example.investsmart.controller;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
public class HealthController {
    private final JdbcTemplate db;
    public HealthController(JdbcTemplate db){this.db=db;}
    @GetMapping("/health") public ResponseEntity<Map<String,String>> health(){try{db.queryForObject("SELECT 1",Integer.class);db.queryForObject("SELECT id FROM security_mutex WHERE id=1",Integer.class);return ResponseEntity.ok(Map.of("status","UP"));}catch(Exception unavailable){return ResponseEntity.status(503).body(Map.of("status","UNAVAILABLE"));}}
}
