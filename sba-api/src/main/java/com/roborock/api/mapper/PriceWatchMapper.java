package com.roborock.api.mapper;

import com.roborock.api.dto.entity.PriceWatchEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PriceWatchMapper {

    @Select("SELECT * FROM T_PRICE_WATCH")
    List<PriceWatchEntity> findAll();

    @Select("SELECT * FROM T_PRICE_WATCH WHERE CREATE_TIME >= NOW() - INTERVAL '${days} DAYS' ORDER BY CREATE_TIME")
    List<PriceWatchEntity> findByIntervalDays(int days);
}
