package com.planit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @NotBlank(message = "*아이디를 입력해주세요.")
    @Size.List({
        @Size(min = 4, message = "*아이디가 너무 짧습니다"),
        @Size(max = 20, message = "*올바른 아이디 형식을 입력해주세요. 아이디는 4자 이상, 20자 이하이며, 영문 대소문자와 숫자, _ 만 포함해야 합니다")
    })
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "*올바른 아이디 형식을 입력해주세요. 아이디는 4자 이상, 20자 이하이며, 영문 대소문자와 숫자, _ 만 포함해야 합니다")
    @Column(name = "login_id", nullable = false, length = 20, unique = true)
    private String loginId;

    @NotBlank(message = "*비밀번호를 입력해주세요.")
    @Size(min = 8, max = 20, message = "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다.")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$",
        message = "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."
    )
    @Column(nullable = false, length = 255)
    private String password;

    @NotBlank(message = "*닉네임을 입력해주세요")
    @Size(max = 10, message = "*닉네임은 최대 10자까지 작성 가능합니다.")
    @Pattern(regexp = "^[^\\s]+$", message = "*띄어쓰기를 없애주세요")
    @Column(nullable = false, length = 10, unique = true)
    private String nickname;

    @Column(columnDefinition = "json")
    private String preferences;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
