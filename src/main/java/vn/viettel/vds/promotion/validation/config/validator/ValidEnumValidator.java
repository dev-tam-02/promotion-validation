package vn.viettel.vds.promotion.validation.config.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for {@link ValidEnum}.
 *
 * <p>Accepts null and blank/empty strings (caller must add {@code @NotBlank} when needed).
 * For non-blank values, performs a case-insensitive match against the enum's declared constants.</p>
 */
public class ValidEnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Set<String> allowedValues;
    private String message;

    @Override
    public void initialize(ValidEnum annotation) {
        allowedValues = Arrays.stream(annotation.value().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
        message = annotation.message();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null → valid (use @NotNull / @NotBlank separately)
        if (value == null) {
            return true;
        }

        // blank/empty → valid (PATCH "clear" semantics; @NotBlank enforces required on CREATE)
        if (value.isBlank()) {
            return true;
        }

        // Case-insensitive match
        boolean matched = allowedValues.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(value));

        if (!matched) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addConstraintViolation();
        }

        return matched;
    }
}
