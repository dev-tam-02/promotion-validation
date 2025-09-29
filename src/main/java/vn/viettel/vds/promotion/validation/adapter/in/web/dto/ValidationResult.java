package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationResult(
        boolean valid,
        String errorCode,
        String errorMessage
) {
    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .build();
    }

    public static ValidationResult failure(String errorCode, String errorMessage) {
        return ValidationResult.builder()
                .valid(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}