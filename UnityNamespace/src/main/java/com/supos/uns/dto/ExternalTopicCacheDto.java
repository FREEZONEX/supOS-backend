package com.supos.uns.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalTopicCacheDto {


    private String payload;

    private Date lastTime;


}
