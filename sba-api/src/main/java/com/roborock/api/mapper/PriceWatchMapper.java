package com.roborock.api.mapper;

import com.roborock.repo.dto.pricewatch.Channel;
import com.roborock.repo.dto.pricewatch.Platform;
import com.roborock.repo.dto.pricewatch.PriceWatchEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PriceWatchMapper {

    @Select("SELECT * FROM T_PRICE_WATCH WHERE PLATFORM = #{platform} AND CHANNEL = #{channel} AND DATE >= NOW() - INTERVAL '${days} DAYS' ORDER BY DATE")
    List<PriceWatchEntity> findByIntervalDays(@Param("platform") Platform platform, @Param("channel") Channel channel, @Param("days") int days);
}
