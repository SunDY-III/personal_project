package com.smartticket.mock;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class MockLogisticsService {
    public Map<String, Object> queryLogistics(String orderNo) {
        return Map.of("orderNo", orderNo, "status", "运输中", "location", "成都分拣中心",
            "estimatedDelivery", "2026-06-14");
    }
}
