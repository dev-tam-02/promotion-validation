package vn.viettel.vds.promotion.validation.adapter.in.web.dto;

// Compatibility alias for RuleNodeResponse
// This is kept for backward compatibility with existing code
// Please use RuleNodeResponse for new code and response DTOs
// Use RuleNodeRequest for request DTOs
public record RuleNodeDto(
        String id,
        String type,
        String groupLogic,
        String operatorName,
        Integer operatorVersion,
        java.util.Map<String, Object> params,
        String reasonCode,
        java.util.List<String> children,
        Integer order
) {
    // Create from request format
    public static RuleNodeDto fromRequest(RuleNodeRequest request) {
        return new RuleNodeDto(
                request.id(), request.type(), request.groupLogic(),
                request.operatorName(), request.operatorVersion(), request.params(),
                request.reasonCode(), request.children(), request.order()
        );
    }

    // Convert to response format
    public RuleNodeResponse toResponse() {
        return new RuleNodeResponse(id, type, groupLogic, operatorName, operatorVersion, params, reasonCode, children, order);
    }
}