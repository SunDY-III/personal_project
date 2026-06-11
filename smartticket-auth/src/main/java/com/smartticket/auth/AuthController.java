package com.smartticket.auth;

import com.smartticket.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String token = authService.login(body.get("username"), body.get("password"));
        return R.ok(Map.of("token", token));
    }

    @GetMapping("/profile")
    public R<Map<String, Object>> profile(@RequestAttribute("userId") Long userId) {
        return R.ok(authService.profile(userId));
    }
}
