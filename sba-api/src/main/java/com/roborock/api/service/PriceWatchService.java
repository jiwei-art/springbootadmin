package com.roborock.api.service;

import com.roborock.api.mapper.PriceWatchMapper;
import com.roborock.repo.dto.pricewatch.Channel;
import com.roborock.repo.dto.pricewatch.Platform;
import com.roborock.repo.dto.pricewatch.PriceWatchEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PriceWatchService {

    private final PriceWatchMapper priceWatchMapper;

    PriceWatchService(PriceWatchMapper priceWatchMapper) {
        this.priceWatchMapper = priceWatchMapper;
    }

    public List<PriceWatchEntity> getPriceWatchList(Platform platform, Channel channel, int intervalDays) {
        return priceWatchMapper.findByIntervalDays(platform, channel, intervalDays);
    }
}
