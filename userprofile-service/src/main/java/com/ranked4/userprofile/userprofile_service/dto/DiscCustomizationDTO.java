package com.ranked4.userprofile.userprofile_service.dto;

import com.ranked4.userprofile.userprofile_service.model.DiscCustomization;

public record DiscCustomizationDTO(
    String itemCode,
    String displayName,
    String type,
    String value,
    Integer price,
    Boolean availableForPurchase
) {
    public DiscCustomizationDTO(DiscCustomization entity) {
        this(
            entity.getItemCode(),
            entity.getDisplayName(),
            entity.getType(),
            entity.getValue(),
            entity.getPrice(),
            entity.getAvailableForPurchase()
        );
    }

    public static DiscCustomizationDTO fromEntity(DiscCustomization entity) {
        if (entity == null) {
            return null;
        }
        return new DiscCustomizationDTO(entity);
    }
}
