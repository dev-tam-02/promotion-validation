package vn.viettel.vds.promotion.validation.adapter.out.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SupportedOperatorDto {

    @JsonProperty("operatorName")
    private String operatorName;

    @JsonProperty("version")
    private Integer version;

    @JsonProperty("description")
    private String description;

    @JsonProperty("context")
    private String context;

    @JsonProperty("translatorClass")
    private String translatorClass;

    public SupportedOperatorDto() {
    }

    public SupportedOperatorDto(String operatorName, Integer version, String description,
                               String context, String translatorClass) {
        this.operatorName = operatorName;
        this.version = version;
        this.description = description;
        this.context = context;
        this.translatorClass = translatorClass;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getTranslatorClass() {
        return translatorClass;
    }

    public void setTranslatorClass(String translatorClass) {
        this.translatorClass = translatorClass;
    }
}