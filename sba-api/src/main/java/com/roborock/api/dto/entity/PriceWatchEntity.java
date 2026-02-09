package com.roborock.api.dto.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class PriceWatchEntity {
    private String id;
    private String countryCode;
    private String price;
    private Date createTime;

}
