package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidateObjectValidityRequest(
        String objectType,
        String objectId,
        String objectCode,
        OffsetDateTime currentDateTime
) {
}