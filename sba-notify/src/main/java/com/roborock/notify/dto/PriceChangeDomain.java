package com.roborock.notify.dto;

import lombok.Data;

import java.util.Date;

@Data
public class PriceChangeDomain {
    private String platform;
    private String channel;
    private String countryCode;
    private Date date;
    private String priceChange;
}
