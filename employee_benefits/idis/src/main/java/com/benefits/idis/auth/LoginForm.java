package com.benefits.idis.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginForm {

    @NotBlank(message = "이름을 입력하세요")
    private String name;

    @NotBlank(message = "전화번호를 입력하세요")
    @Pattern(regexp = "^$|^01[016789]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
    private String phone;
}
