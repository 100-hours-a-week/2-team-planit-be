package com.planit.domain.user.exception;

import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;

public class DuplicateNicknameException extends BusinessException {

    public DuplicateNicknameException() {
        super(ErrorCode.USER_DUPLICATE_NICKNAME);
    }
}
