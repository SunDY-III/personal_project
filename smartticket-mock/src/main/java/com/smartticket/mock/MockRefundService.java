package com.smartticket.mock;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class MockRefundService {
    public Map<String, Object> checkRefundPolicy(String orderNo, String reason) {
        boolean eligible = orderNo != null && !orderNo.isBlank();
        return Map.of("eligible", eligible, "reason", eligible ? "符合7天无理由退款条件" : "订单不存在");
    }

    public Map<String, Object> createRefund(String orderNo, double amount, String reason) {
        return Map.of("refundId", "RF" + System.currentTimeMillis(), "status", "已提交审核", "amount", amount);
    }

    public Map<String, Object> issueCoupon(Long userId, double amount, String reason) {
        return Map.of("couponId", "CP" + System.currentTimeMillis(), "amount", amount, "status", "已发放");
    }
}
