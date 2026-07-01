package com.cotask.controller;

import com.cotask.entity.User;
import com.cotask.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            User user = authService.register(
                    request.get("email"),
                    request.get("name"),
                    request.get("password")
            );
            return new ResponseEntity<>(Map.of(
                    "message", "注册成功",
                    "userId", user.getId()
            ), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = authService.login(
                    request.get("email"),
                    request.get("password")
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
}
