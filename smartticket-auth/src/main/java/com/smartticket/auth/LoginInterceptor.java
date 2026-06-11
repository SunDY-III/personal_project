package com.smartticket.auth;

import com.smartticket.common.BizException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {
    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        if ("OPTIONS".equals(req.getMethod())) return true;
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer "))
            throw new BizException(401, "未登录");
        try {
            Claims claims = jwtUtils.parse(auth.substring(7));
            req.setAttribute("userId", jwtUtils.getUserId(claims));
            req.setAttribute("role", jwtUtils.getRole(claims));
            return true;
        } catch (Exception e) {
            throw new BizException(401, "Token无效或已过期");
        }
    }
}
