package com.hexix.homeassistant.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Enthält die Attribute einer Entität. Bekannte Felder sind direkt gemappt,
 * alle anderen werden in der additionalAttributes Map gesammelt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributesDto {

    @JsonProperty("friendly_name")
    private String friendlyName;

    @JsonProperty("supported_features")
    private Integer supportedFeatures;

    @JsonProperty("device_class")
    private String deviceClass;

    @JsonProperty("unit_of_measurement")
    private String unitOfMeasurement;

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("entity_picture")
    private String entityPicture;

    @JsonProperty("installed_version")
    private String installedVersion;

    @JsonProperty("latest_version")
    private String latestVersion;

    private Map<String, Object> additionalAttributes = new HashMap<>();

    // Standard-Getter und -Setter

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public Integer getSupportedFeatures() {
        return supportedFeatures;
    }

    public void setSupportedFeatures(Integer supportedFeatures) {
        this.supportedFeatures = supportedFeatures;
    }

    public String getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(String deviceClass) {
        this.deviceClass = deviceClass;
    }

    public String getUnitOfMeasurement() {
        return unitOfMeasurement;
    }

    public void setUnitOfMeasurement(String unitOfMeasurement) {
        this.unitOfMeasurement = unitOfMeasurement;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getEntityPicture() {
        return entityPicture;
    }

    public void setEntityPicture(String entityPicture) {
        this.entityPicture = entityPicture;
    }

    public String getInstalledVersion() {
        return installedVersion;
    }

    public void setInstalledVersion(String installedVersion) {
        this.installedVersion = installedVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    /**
     * Sammelt alle nicht explizit definierten JSON-Attribute in einer Map.
     *
     * @param name  Der Schlüssel des Attributs.
     * @param value Der Wert des Attributs.
     */
    @JsonAnySetter
    public void addAdditionalAttribute(String name, Object value) {
        this.additionalAttributes.put(name, value);
    }

    public Map<String, Object> getAdditionalAttributes() {
        return additionalAttributes;
    }
}
