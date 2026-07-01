package com.cotask.service;

import com.cotask.entity.User;
import java.util.Map;

public interface AuthService {
    User register(String email, String name, String password);
    Map<String, Object> login(String email, String password);
}