package io.github.takgeun.shop.global.validation;

public class ValidationGroups {
    public interface Required {}        // NotBlank 같은 필수
    public interface Format {}          // Size, Pattern, Email 같은 형식/길이
}
