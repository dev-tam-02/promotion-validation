package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import jakarta.validation.GroupSequence;

/**
 * Validation groups for Fail-Fast validation pattern.
 * <p>
 * Validation order:
 * 1. RequiredCheck → @NotNull → {FIELD}_REQUIRED
 * 2. EmptyCheck    → @NotBlank → {FIELD}_EMPTY
 * 3. LengthCheck   → @Size     → {FIELD}_LENGTH_EXCEEDED
 * 4. FormatCheck   → Custom    → {FIELD}_INVALID
 * <p>
 * Logic:
 * - If field null → return {FIELD}_REQUIRED, stop
 * - If field empty → return {FIELD}_EMPTY, stop
 * - If field too long → return {FIELD}_LENGTH_EXCEEDED, stop
 * - If format invalid → return {FIELD}_INVALID
 */
public final class ValidationGroups {

    private ValidationGroups() {
        // Utility class - prevent instantiation
    }

    /**
     * Group 1: Check if field is present (not null).
     * Used with @NotNull annotation.
     * Error: {FIELD}_REQUIRED
     */
    public interface RequiredCheck {
    }

    /**
     * Group 2: Check if field is not empty/blank.
     * Used with @NotBlank, @NotEmpty annotations.
     * Error: {FIELD}_EMPTY
     */
    public interface EmptyCheck {
    }

    /**
     * Group 3: Check field length/size constraints.
     * Used with @Size, @Length annotations.
     * Error: {FIELD}_LENGTH_EXCEEDED
     */
    public interface LengthCheck {
    }

    /**
     * Group 4: Check field format/pattern.
     * Used with @Pattern, @Id, custom validators.
     * Error: {FIELD}_INVALID
     */
    public interface FormatCheck {
    }

    /**
     * Ordered sequence for Fail-Fast validation.
     * Validation stops at the first group that fails.
     * <p>
     * Order: RequiredCheck → EmptyCheck → LengthCheck → FormatCheck
     */
    @GroupSequence({RequiredCheck.class, EmptyCheck.class, LengthCheck.class, FormatCheck.class})
    public interface OrderedChecks {
    }
}
