package com.supos.uns.vo;

import com.supos.common.dto.JsonResult;
import lombok.Data;

public class RemoveResult extends JsonResult<RemoveResult.RemoveTip> {

    public RemoveResult() {
    }

    public RemoveResult(int code, String msg) {
        super(code, msg);
    }

    public RemoveResult(int code, String msg, RemoveTip data) {
        super(code, msg, data);
    }

    @Data
    public static class RemoveTip {
        Integer refs;
    }
}
