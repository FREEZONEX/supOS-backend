package com.supos.uns.config;

import java.util.LinkedHashMap;

public class JsonMapTypeHandler extends JsonBaseTypeHandler<LinkedHashMap<String, Object>> {

    public JsonMapTypeHandler( ) {
        super(true);
    }
}
