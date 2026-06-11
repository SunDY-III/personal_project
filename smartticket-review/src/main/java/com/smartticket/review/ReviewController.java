package com.smartticket.review;

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
    public R<?> list() { return R.ok(reviewService.list()); }

    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) { return R.ok(reviewService.detail(id)); }

    @PostMapping("/{id}/approve")
    public R<?> approve(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest req) {
        Long reviewerId = (Long) req.getAttribute("userId");
        reviewService.approve(id, reviewerId, body.getOrDefault("comment", "审核通过"));
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    public R<?> reject(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest req) {
        Long reviewerId = (Long) req.getAttribute("userId");
        reviewService.reject(id, reviewerId, body.getOrDefault("comment", "审核拒绝"));
        return R.ok();
    }
}
