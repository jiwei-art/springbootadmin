package com.roborock.notify.service;

import com.roborock.notify.config.LocalCacheName;
import com.roborock.notify.dto.PriceChangeDomain;
import com.roborock.notify.mapper.PriceWatchMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotifyService {
    private final PriceWatchMapper priceWatchMapper;

    NotifyService(PriceWatchMapper priceWatchMapper) {
        this.priceWatchMapper = priceWatchMapper;
    }

    @Cacheable(LocalCacheName.PRICE_CHANGE)
    public List<PriceChangeDomain> getPriceChange() {
        return priceWatchMapper.getPriceWatch();
    }
}
