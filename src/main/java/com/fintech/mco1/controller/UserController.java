package com.fintech.mco1.controller;

import com.fintech.mco1.dto.ApiResponse;
import com.fintech.mco1.dto.UserResponse;
import com.fintech.mco1.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Current user fetched", UserResponse.from(user)));
    }
}
