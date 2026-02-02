package com.planit.domain.user.exception;

import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;

public class DuplicateLoginIdException extends BusinessException {

    public DuplicateLoginIdException() {
        super(ErrorCode.USER_DUPLICATE_LOGIN_ID);
    }
}
