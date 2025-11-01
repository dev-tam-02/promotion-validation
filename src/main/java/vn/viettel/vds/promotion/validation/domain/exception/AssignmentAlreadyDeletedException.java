package vn.viettel.vds.promotion.validation.domain.exception;

/**
 * Exception được throw khi assignment đã bị xóa (optimistic locking conflict).
 */
public class AssignmentAlreadyDeletedException extends RuntimeException {

    private final String assignmentId;

    public AssignmentAlreadyDeletedException(String assignmentId) {
        super(String.format("Assignment has been deleted by another process: id=%s", assignmentId));
        this.assignmentId = assignmentId;
    }

    public String getAssignmentId() {
        return assignmentId;
    }
}
