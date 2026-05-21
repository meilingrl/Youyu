package com.campusmarket.backend.common.exception;

import com.campusmarket.backend.common.api.ResultCode;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(ResultCode.FORBIDDEN, message);
    }
}

