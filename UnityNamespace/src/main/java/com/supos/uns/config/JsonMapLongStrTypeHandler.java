package com.supos.uns.config;

import java.util.TreeMap;

public class JsonMapLongStrTypeHandler extends JsonBaseTypeHandler<TreeMap<Long, String>> {

    public JsonMapLongStrTypeHandler() {
        super(true);
    }
}
