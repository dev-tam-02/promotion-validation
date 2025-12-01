package vn.viettel.vds.promotion.validation.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.DeleteAssignmentRequest;
import vn.viettel.vds.promotion.validation.adapter.in.web.dto.DeleteAssignmentResponse;
import vn.viettel.vds.promotion.validation.application.service.AssignmentService;

/**
 * REST Controller for Assignment management operations.
 * <p>
 * Implements SRS PRM_KBNV_API_VALD008 - Delete validation rule assignment from object.
 */
@RestController
@Validated
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/assignments")
@Tag(name = "Assignments", description = "Validation rule assignment management API")
public class AssignmentController {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentController.class);

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /**
     * Delete validation rule assignment from object.
     * <p>
     * Implements SRS PRM_KBNV_API_VALD008.
     * <p>
     * Steps:
     * 1. Validate input (validation_rule_id, object_id)
     * 2. Verify validation rule exists
     * 3. Verify object (campaign) exists
     * 4. Verify assignment exists for rule and object combination
     * 5. Soft delete: update deleted_at, deleted_by, version+1
     * 6. Insert into validation_rules_assignment_deleted
     *
     * @param request   the delete assignment request containing validation_rule_id and object_id
     * @param userId    the user performing the deletion
     * @return DeleteAssignmentResponse with success code and message
     */
    @Operation(
            summary = "Delete validation rule assignment from object",
            description = "Remove a validation rule from an object (campaign). " +
                    "This performs a soft delete by archiving the assignment record."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
            @ApiResponse(responseCode = "404", description = "Validation rule, object, or assignment not found")
    })
    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public DeleteAssignmentResponse deleteAssignment(
            @Valid @RequestBody DeleteAssignmentRequest request,
            @Parameter(description = "User making the request")
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId) {

        logger.info("Delete assignment request received: validationRuleId={}, objectId={}, userId={}",
                request.getValidationRuleId(), request.getObjectId(), userId);

        // Trim whitespace from IDs
        String validationRuleId = request.getValidationRuleId().trim();
        String objectId = request.getObjectId().trim();

        // Perform delete operation
        String deletedAssignmentId = assignmentService.deleteAssignmentByRuleAndObject(
                validationRuleId, objectId, userId);

        logger.info("Assignment deleted successfully: assignmentId={}", deletedAssignmentId);

        return DeleteAssignmentResponse.success(deletedAssignmentId);
    }
}
