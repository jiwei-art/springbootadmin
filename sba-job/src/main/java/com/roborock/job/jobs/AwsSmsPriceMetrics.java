package com.roborock.job.jobs;

import com.roborock.repo.dto.pricewatch.Channel;
import com.roborock.repo.dto.pricewatch.Platform;
import com.roborock.job.service.PriceWatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AwsSmsPriceMetrics {

    private final String AWS_URL = "https://s3.amazonaws.com/aws-messaging-pricing-information/TextMessageOutbound/prices.json";

    // 存储不同国家的价格，Key 为国家代码（如 CN, US）
    private final Map<String, AtomicReference<Double>> priceMap = new ConcurrentHashMap<>();
    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final PriceWatchService priceWatchService;

    AwsSmsPriceMetrics(PriceWatchService priceWatchService, RestClient restClient, JsonMapper jsonMapper) {
        this.priceWatchService = priceWatchService;
        this.restClient = restClient; // Fixed: Removed unnecessary semicolon
        this.jsonMapper = jsonMapper;
    }

    /**
     * 每小时抓取一次数据 (3600000ms)
     */
    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    public void fetchAwsPrices() {
        try {
            String resp = restClient.get().uri(AWS_URL).retrieve().body(String.class);
            JsonNode root = jsonMapper.readTree(resp);
            JsonNode prices = root.get("SMS").get("countryPrices");
            for (Map.Entry<String, JsonNode> item : prices.properties()) {
                String countryCode = item.getKey();
                JsonNode transactionalArray = item.getValue().get("transactional");
                if (transactionalArray.isArray() && !transactionalArray.isEmpty()) {
                    // 获取数组第一个对象的 domesticPrice
                    double price = transactionalArray.path(0).path("domesticPrice").asDouble();
                    // 执行 Prometheus 上报
                    priceWatchService.insert(Platform.AWS, Channel.SMS, countryCode, price);
                }
            }
        } catch (Exception e) {
            // 这里可以记录日志或上报抓取失败的指标
            System.err.println("Failed to fetch AWS prices: " + e.getMessage());
        }
    }
}
