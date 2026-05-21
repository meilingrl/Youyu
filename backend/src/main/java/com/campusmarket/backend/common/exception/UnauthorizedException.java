package com.campusmarket.backend.common.exception;

import com.campusmarket.backend.common.api.ResultCode;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(ResultCode.UNAUTHORIZED, message);
    }
}

