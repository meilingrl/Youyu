package com.youyu.backend.common.exception;

import com.youyu.backend.common.api.ResultCode;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(ResultCode.UNAUTHORIZED, message);
    }
}

