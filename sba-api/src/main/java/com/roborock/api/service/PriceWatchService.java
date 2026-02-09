package com.roborock.api.service;


import com.roborock.api.dto.entity.PriceWatchEntity;
import com.roborock.api.mapper.PriceWatchMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PriceWatchService {

    private final PriceWatchMapper priceWatchMapper;

    PriceWatchService(PriceWatchMapper priceWatchMapper) {
        this.priceWatchMapper = priceWatchMapper;
    }

    public List<PriceWatchEntity> getPriceWatchList(int intervalDays) {
        return priceWatchMapper.findByIntervalDays(intervalDays);
    }
}
