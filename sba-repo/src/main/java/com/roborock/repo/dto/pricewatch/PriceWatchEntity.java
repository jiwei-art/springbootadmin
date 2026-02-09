package com.roborock.repo.dto.pricewatch;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class PriceWatchEntity {
    private long id;
    private Platform platform;
    private Channel channel;
    private String countryCode;
    private double price;
    private Date date;
    private Date createTime;
    private Date updateTime;

}
