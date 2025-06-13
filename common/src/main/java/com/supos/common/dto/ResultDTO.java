package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultDTO<T> implements Serializable {

    private static final long serialVersionUID = 1l;

    private int code;

    private String msg;

    private T data;

    public static <T> ResultDTO successWithData(T data) {
        return ResultDTO.builder().code(200).data(data).build();
    }

    public static <T> ResultDTO success(String msg) {
        return ResultDTO.builder().code(200).msg(msg).build();
    }

    public boolean isSuccess() {
        return this.code == 200;
    }

    public static <T> ResultDTO fail(String msg) {
        return ResultDTO.builder().code(500).msg(msg).build();
    }

}
