package com.planit.domain.keywordalert.exception;

public class DuplicateKeywordException extends RuntimeException {

    public DuplicateKeywordException() {
        super("이미 등록된 키워드입니다.");
    }
}

