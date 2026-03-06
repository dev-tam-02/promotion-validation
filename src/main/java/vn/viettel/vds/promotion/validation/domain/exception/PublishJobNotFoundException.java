package vn.viettel.vds.promotion.validation.domain.exception;

import com.promix.platform.core.exception.ResourceNotFoundException;

/**
 * Exception thrown when a publish job is not found.
 */
public class PublishJobNotFoundException extends ResourceNotFoundException {

    private static final String ERROR_CODE = "PUBLISH_JOB_NOT_FOUND";

    public PublishJobNotFoundException(String jobId) {
        super(ERROR_CODE, "PublishJob", jobId);
    }
}
