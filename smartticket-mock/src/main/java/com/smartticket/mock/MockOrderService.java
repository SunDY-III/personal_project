package com.smartticket.mock;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class MockOrderService {
    public Map<String, Object> queryOrder(String orderNo) {
        return Map.of("orderNo", orderNo, "status", "已发货", "amount", 299.00,
            "product", "蓝牙耳机 Pro", "createdAt", "2026-06-01");
    }
}
