package com.supos.gateway.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenVo {

    private String token;

    private long expires_in;
}
