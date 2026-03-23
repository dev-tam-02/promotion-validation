package vn.viettel.vds.promotion.validation.domain.enums;

/**
 * Enum defining supported validation rule contexts.
 * Each context represents the business scenario in which validation rules apply.
 */
public enum RuleContextType {

    ORDER("Đơn hàng"),
    CUSTOMER("Khách hàng"),
    VOUCHER("Voucher"),
    CAMPAIGN("Chiến dịch"),
    PRODUCT("Sản phẩm"),
    REDEMPTION("Đổi thưởng");

    private final String label;

    RuleContextType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
