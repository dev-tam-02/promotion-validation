package vn.viettel.vds.promotion.validation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tenant")
public class TenantProperties {

    private String defaultTenantId = "default";
    private String compilerId = "validation-service-compiler-v1";

    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    public String getCompilerId() {
        return compilerId;
    }

    public void setCompilerId(String compilerId) {
        this.compilerId = compilerId;
    }
}
