package com.roborock.notify.healthindicator;

import com.roborock.notify.dto.PriceChangeDomain;
import com.roborock.notify.service.NotifyService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PriceChangeHealthIndicator implements HealthIndicator {

    private final NotifyService notifyService;

    public PriceChangeHealthIndicator(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @Override
    public Health health() {
        try {
            List<PriceChangeDomain> changes = notifyService.getPriceChange();

            if (!changes.isEmpty()) {
                return Health.unknown()
                        .withDetail("message", "检测到云供应商价格变动！")
                        .withDetail("changes", changes)
                        .build();
            }

            return Health.up().withDetail("message", "价格平稳").build();
        } catch (Exception e) {
            return Health.up().withDetail("error", "价格检查失败: " + e.getMessage()).build();
        }
    }
}