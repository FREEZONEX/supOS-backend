package com.supos.uns.util;

import com.alibaba.fastjson.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

public class SseJsonBuilder implements SseEmitter.SseEventBuilder {
    Set<ResponseBodyEmitter.DataWithMediaType> dataWithMediaTypeSet;
    JSONObject jsonObject;

    public SseJsonBuilder(String json) {
        dataWithMediaTypeSet = Set.of(new ResponseBodyEmitter.DataWithMediaType(json, MediaType.APPLICATION_JSON));
    }

    public SseJsonBuilder() {
        jsonObject = new JSONObject();
    }

    @Override
    public Set<ResponseBodyEmitter.DataWithMediaType> build() {
        return dataWithMediaTypeSet != null ? dataWithMediaTypeSet :
                Set.of(new ResponseBodyEmitter.DataWithMediaType(jsonObject.toJSONString(), MediaType.APPLICATION_JSON));
    }

    @NotNull
    @Override
    public SseJsonBuilder id(@NotNull String id) {
        jsonObject.put("id", id);
        return this;
    }

    @Override
    public SseJsonBuilder name(String eventName) {
        jsonObject.put("name", eventName);
        return this;
    }

    @Override
    public SseJsonBuilder reconnectTime(long reconnectTimeMillis) {
        jsonObject.put("reconnectTime", reconnectTimeMillis);
        return this;
    }

    @Override
    public SseJsonBuilder comment(String comment) {
        jsonObject.put("comment", comment);
        return this;
    }

    @Override
    public SseJsonBuilder data(Object object) {
        jsonObject.put("object", object);
        return this;
    }

    @Override
    public SseJsonBuilder data(Object object, MediaType mediaType) {
        jsonObject.put("object", object);
        return this;
    }
}
