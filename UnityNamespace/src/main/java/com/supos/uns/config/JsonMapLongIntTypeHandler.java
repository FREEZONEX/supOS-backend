package com.supos.uns.config;

import java.util.Map;

public class JsonMapLongIntTypeHandler extends JsonBaseTypeHandler<Map<Long, Integer>> {

    public JsonMapLongIntTypeHandler() {
        super(true);
    }
}
