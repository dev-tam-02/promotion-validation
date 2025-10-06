package vn.viettel.vds.promotion.validation.domain.valueobject;

import java.util.Objects;

/**
 * Value object representing a tenant identifier
 */
public class TenantId {
    private final String value;

    private TenantId(String value) {
        Objects.requireNonNull(value, "TenantId cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("TenantId cannot be empty");
        }
        this.value = value;
    }

    public static TenantId of(String value) {
        return new TenantId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantId tenantId = (TenantId) o;
        return Objects.equals(value, tenantId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}