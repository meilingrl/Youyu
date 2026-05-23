package com.youyu.backend.common.exception;

import com.youyu.backend.common.api.ResultCode;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(ResultCode.FORBIDDEN, message);
    }
}

