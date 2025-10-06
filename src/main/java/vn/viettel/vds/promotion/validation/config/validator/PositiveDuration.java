package vn.viettel.vds.promotion.validation.config.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PositiveDurationValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PositiveDuration {
    String message() default "Duration must be positive";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}