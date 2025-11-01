package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.application.port.in.DeleteValidationRuleAssignmentUseCase;
import vn.viettel.vds.promotion.validation.config.validator.ValidRuleId;

/**
 * REST Controller cho Validation Rule Assignment operations.
 * Quản lý việc gán validation rules cho các đối tượng (campaign, voucher, etc.)
 */
@Slf4j
@RestController
@Validated
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/validation-rules")
@Tag(name = "Validation Rule Assignments", description = "API quản lý gán validation rules cho objects")
@RequiredArgsConstructor
public class ValidationRuleAssignmentController {

    private final DeleteValidationRuleAssignmentUseCase deleteAssignmentUseCase;

    /**
     * Xóa validation rule assignment.
     * DELETE /api/v1/validation-rules/{validationRuleId}/assignments/{objectId}
     *
     * @param validationRuleId ID của validation rule
     * @param objectId         ID của object (campaign, voucher, etc.)
     */
    @Operation(
            summary = "Xóa validation rule assignment",
            description = "Xóa (soft delete) việc gán validation rule cho object cụ thể"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "400", description = "Invalid request - lỗi validation input"),
            @ApiResponse(responseCode = "404", description = "Validation rule hoặc assignment không tồn tại"),
            @ApiResponse(responseCode = "409", description = "Assignment đã bị xóa (optimistic locking conflict)")
    })
    @DeleteMapping("/{validationRuleId}/assignments/{objectId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteAssignment(
            @Parameter(description = "ID của validation rule (UUID format)", required = true)
            @PathVariable
            @ValidRuleId
            @NotBlank(message = "VALIDATION_RULE_ID_REQUIRED: validation_rule_id không được để trống")
            String validationRuleId,

            @Parameter(description = "ID của object (UUID format)", required = true)
            @PathVariable
            @NotBlank(message = "OBJECT_ID_REQUIRED: object_id không được để trống")
            String objectId) {

        log.info("Received delete assignment request: validationRuleId={}, objectId={}",
                validationRuleId, objectId);

        // Trim whitespace
        String trimmedValidationRuleId = validationRuleId.trim();
        String trimmedObjectId = objectId.trim();

        // Call use case
        deleteAssignmentUseCase.deleteAssignment(trimmedValidationRuleId, trimmedObjectId);

        log.info("Delete assignment completed: validationRuleId={}, objectId={}",
                trimmedValidationRuleId, trimmedObjectId);
    }
}
