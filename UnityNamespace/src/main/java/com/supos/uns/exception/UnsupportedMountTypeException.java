package com.supos.uns.exception;

import com.supos.common.exception.BuzException;

public class UnsupportedMountTypeException extends BuzException {

    public UnsupportedMountTypeException(int code, String msg, Object... params) {
        super(code, msg, params);
    }
}
