package com.roborock.job.service;


import com.roborock.repo.dto.pricewatch.Channel;
import com.roborock.repo.dto.pricewatch.Platform;
import com.roborock.job.mapper.PriceWatchMapper;
import com.roborock.repo.dto.pricewatch.PriceWatchEntity;
import org.springframework.stereotype.Service;

@Service
public class PriceWatchService {

    private final PriceWatchMapper priceWatchMapper;

    PriceWatchService(PriceWatchMapper priceWatchMapper) {
        this.priceWatchMapper = priceWatchMapper;
    }

    public int insert(Platform platform, Channel channel, String countryCode, double price) {
        PriceWatchEntity entity = new PriceWatchEntity();
        entity.setPlatform(platform);
        entity.setChannel(channel);
        entity.setPrice(price);
        entity.setCountryCode(countryCode);
        return priceWatchMapper.insert(entity);
    }
}
