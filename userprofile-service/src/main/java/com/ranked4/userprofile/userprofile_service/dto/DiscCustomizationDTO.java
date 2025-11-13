package com.ranked4.userprofile.userprofile_service.dto;

import com.ranked4.userprofile.userprofile_service.model.DiscCustomization;

public class DiscCustomizationDTO {
    private String itemCode;
    private String displayName;
    private String type;
    private String value;

    public DiscCustomizationDTO(DiscCustomization entity) {
        this.itemCode = entity.getItemCode();
        this.displayName = entity.getDisplayName();
        this.type = entity.getType();
        this.value = entity.getValue();
    }

    public DiscCustomizationDTO() {}
    
    public static DiscCustomizationDTO fromEntity(DiscCustomization entity) {
        if (entity == null) {
            return null;
        }
        return new DiscCustomizationDTO(entity);
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
