package com.supos.uns.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.gson.*;

import java.io.IOException;
import java.lang.reflect.Type;

public class ToStringSerializer extends JsonSerializer<Object> implements com.google.gson.JsonSerializer<Object> {
    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (value.getClass().isArray()) {
            Object[] values = (Object[]) value;
            String[] array = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                array[i] = String.valueOf(values[i]);
            }
            gen.writeArray(array, 0, array.length);
        } else {
            gen.writeString(value.toString());
        }
    }

    @Override
    public JsonElement serialize(Object value, Type typeOfSrc, JsonSerializationContext context) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value.getClass().isArray()) {
            Object[] values = (Object[]) value;
            JsonArray array = new JsonArray(values.length);
            for (Object v : values) {
                array.add(String.valueOf(v));
            }
            return array;
        } else {
            return new JsonPrimitive(value.toString());
        }
    }
}
