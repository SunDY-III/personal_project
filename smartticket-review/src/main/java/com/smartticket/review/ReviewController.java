package com.smartticket.review;

import com.smartticket.common.BizException;
import com.smartticket.common.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public R<?> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestAttribute("role") String role) {
        checkReviewerRole(role);
        return R.ok(reviewService.list(page, size));
    }

    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id, @RequestAttribute("role") String role) {
        checkReviewerRole(role);
        return R.ok(reviewService.detail(id));
    }

    @PostMapping("/{id}/approve")
    public R<?> approve(@PathVariable Long id, @RequestBody Map<String, String> body,
            HttpServletRequest req) {
        Long reviewerId = (Long) req.getAttribute("userId");
        String role = (String) req.getAttribute("role");
        checkReviewerRole(role);
        reviewService.approve(id, reviewerId, body.getOrDefault("comment", "审核通过"));
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    public R<?> reject(@PathVariable Long id, @RequestBody Map<String, String> body,
            HttpServletRequest req) {
        Long reviewerId = (Long) req.getAttribute("userId");
        String role = (String) req.getAttribute("role");
        checkReviewerRole(role);
        reviewService.reject(id, reviewerId, body.getOrDefault("comment", "审核拒绝"));
        return R.ok();
    }

    private void checkReviewerRole(String role) {
        if (!"ADMIN".equals(role) && !"REVIEWER".equals(role) && !"STAFF".equals(role))
            throw new BizException(403, "无审核权限");
    }
}
