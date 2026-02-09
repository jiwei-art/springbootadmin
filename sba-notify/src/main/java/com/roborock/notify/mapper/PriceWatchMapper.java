package com.roborock.notify.mapper;

import com.roborock.notify.dto.PriceChangeDomain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PriceWatchMapper {

    @Select("""
                SELECT t1.PLATFORM, t1.CHANNEL, t1.COUNTRY_CODE, (t1.PRICE-t2.PRICE) as PRICE_CHANGE, t1.DATE
                FROM T_PRICE_WATCH t1
                INNER JOIN T_PRICE_WATCH t2 ON t1.PLATFORM = t2.PLATFORM
                    AND t1.CHANNEL = t2.CHANNEL
                    AND t1.COUNTRY_CODE = t2.COUNTRY_CODE
                WHERE t1.DATE = CURRENT_DATE
                  AND t2.DATE = CURRENT_DATE - 1
                  AND ABS(t1.PRICE - t2.PRICE) != 0
            """)
    List<PriceChangeDomain> getPriceWatch();
}
