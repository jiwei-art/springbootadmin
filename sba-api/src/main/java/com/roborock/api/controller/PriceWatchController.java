package com.roborock.api.controller;

import com.roborock.api.service.PriceWatchService;
import com.roborock.repo.dto.pricewatch.Channel;
import com.roborock.repo.dto.pricewatch.Platform;
import com.roborock.repo.dto.pricewatch.PriceWatchEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/price-watch")
public class PriceWatchController {

    private final PriceWatchService priceWatchService;

    PriceWatchController(PriceWatchService priceWatchService) {
        this.priceWatchService = priceWatchService;
    }

    @CrossOrigin(origins = "http://127.0.0.1:7005")
    @GetMapping
    public List<PriceWatchEntity> getPriceWatch(@RequestParam(value = "platform") Platform platform,
                                                @RequestParam(value = "channel") Channel channel,
                                                @RequestParam(value = "intervalDays") int intervalDays) {
        return priceWatchService.getPriceWatchList(platform, channel, intervalDays);
    }

}
