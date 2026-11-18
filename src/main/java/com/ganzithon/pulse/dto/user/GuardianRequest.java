package com.ganzithon.pulse.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuardianRequest(
        @NotBlank(message = "보호자 이름을 입력해 주세요.") @Size(max = 20, message = "보호자 이름은 20자 이내예요.") String name,
        @NotBlank(message = "보호자 번호를 입력해 주세요.")
        @Pattern(regexp = "^01\\d{8,9}$", message = "휴대폰 번호를 확인해 주세요.") String phone) {

    /** 번호는 숫자만 남긴 뒤 검사한다. 프론트와 같은 규칙 */
    public GuardianRequest {
        name = name == null ? null : name.strip();
        phone = phone == null ? null : phone.replaceAll("[^0-9]", "");
    }
}
