package com.roborock.api.controller;

import com.roborock.api.dto.entity.PriceWatchEntity;
import com.roborock.api.service.PriceWatchService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/price-watch")
public class PriceWatchController {

    private final PriceWatchService priceWatchService;

    PriceWatchController(PriceWatchService priceWatchService) {
        this.priceWatchService = priceWatchService;
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping
    public List<PriceWatchEntity> getPriceWatch(@RequestParam(value = "intervalDays") int intervalDays) {
        return priceWatchService.getPriceWatchList(intervalDays);
    }

}
