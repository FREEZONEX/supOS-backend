package com.supos.uns.config;

import com.supos.common.utils.JsonUtil;
import com.supos.uns.util.ClassTypeUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class JsonBaseTypeHandler<T> extends BaseTypeHandler<T> {
    private final String type;
    private final Type valueType;

    public JsonBaseTypeHandler(boolean jsonB) {
        this.type = jsonB ? "jsonb" : "json";
        valueType = ClassTypeUtils.getParameterizedType(getClass())[0];
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType(type);
        try {
            jsonObject.setValue(JsonUtil.jackToJson(parameter));
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
        ps.setObject(i, jsonObject);
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private T parseJson(String json) {
        if (json == null) return null;
        try {
            return JsonUtil.fromJson(json, valueType);
        } catch (Exception e) {
            throw new RuntimeException("JSON解析失败", e);
        }
    }
}
