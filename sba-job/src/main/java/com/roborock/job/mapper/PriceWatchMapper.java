package com.roborock.job.mapper;

import com.roborock.repo.dto.pricewatch.PriceWatchEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface PriceWatchMapper {

    @Insert("""
            INSERT INTO T_PRICE_WATCH(PLATFORM, CHANNEL, COUNTRY_CODE, PRICE) VALUES (#{platform}, #{channel},
            #{countryCode}, #{price}) ON CONFLICT (PLATFORM, CHANNEL, COUNTRY_CODE, DATE) DO NOTHING
        """)
    @Options(keyProperty = "id", useGeneratedKeys = true)
    int insert(PriceWatchEntity price);
}
