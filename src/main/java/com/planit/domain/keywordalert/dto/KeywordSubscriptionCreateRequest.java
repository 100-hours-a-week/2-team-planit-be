package com.planit.domain.keywordalert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class KeywordSubscriptionCreateRequest {

    @NotBlank(message = "키워드를 입력해주세요.")
    @Size(min = 2, max = 10, message = "키워드는 2자 이상 10자 이하로 입력해주세요.")
    @Pattern(
            regexp = "^(?!.*[ㄱ-ㅎㅏ-ㅣ])[가-힣A-Za-z]{2,10}$",
            message = "키워드는 한글/영문만 가능하며 숫자, 공백, 특수문자, 초성은 사용할 수 없습니다."
    )
    private String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}

