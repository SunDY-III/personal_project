package com.smartticket.auth;

import com.smartticket.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JdbcTemplate jdbc;
    private final JwtUtils jwtUtils;

    public String login(String username, String password) {
        if (username == null || password == null) throw new BizException(400, "用户名密码不能为空");
        String hash = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        var list = jdbc.queryForList(
            "SELECT id, username, role FROM sys_user WHERE username = ? AND password_hash = ? AND status = 1",
            username, hash);
        if (list.isEmpty()) throw new BizException(401, "用户名或密码错误");
        var row = list.get(0);
        return jwtUtils.generate(((Number)row.get("id")).longValue(), (String)row.get("username"), (String)row.get("role"));
    }

    public Map<String, Object> profile(Long userId) {
        return jdbc.queryForMap("SELECT id, username, role FROM sys_user WHERE id = ?", userId);
    }
}
