package com.roborock.repo.dto.pricewatch;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class PriceWatchEntity {
    private long id;
    private String countryCode;
    private double price;
    private Date createTime;

}
