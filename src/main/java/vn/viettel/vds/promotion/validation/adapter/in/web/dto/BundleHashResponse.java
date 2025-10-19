package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

/**
 * Response containing bundle hash and metadata for an object.
 * The bundle hash is the SHA256 hash of the compiled Drools rule artifact.
 */
@Schema(description = "Bundle hash information for an object")
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BundleHashResponse(

        @Schema(description = "Object type", example = "campaign")
        String objectType,

        @Schema(description = "Object identifier/key", example = "CAMPAIGN-001")
        String objectId,

        @Schema(description = "SHA256 hash of compiled rule bundle",
                example = "e5a7b8c9d4f3a2b1c8d7e6f5a4b3c2d1a9b8c7d6e5f4a3b2c1d0")
        String bundleHash,

        @Schema(description = "Rule version number", example = "3")
        Integer ruleVersion,

        @Schema(description = "Assignment version number", example = "1")
        Integer assignmentVersion,

        @Schema(description = "When the bundle was compiled")
        Instant compiledAt
) {
}
