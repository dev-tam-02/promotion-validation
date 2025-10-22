package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to get bundle hash for an object (campaign, voucher, etc.).
 * Used to resolve the compiled rule bundle hash from object identifier.
 */
@Schema(description = "Request to get bundle hash for an object")
public record BundleHashRequest(

        @Schema(description = "Object type (campaign, voucher, tier, reward)", example = "campaign", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Object type is required")
        String objectType,

        @Schema(description = "Object identifier/key", example = "CAMPAIGN-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Object ID is required")
        String objectId
) {
}
