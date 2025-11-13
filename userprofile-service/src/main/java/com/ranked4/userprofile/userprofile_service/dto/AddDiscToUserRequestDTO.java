package com.ranked4.userprofile.userprofile_service.dto;

public class AddDiscToUserRequestDTO {
    private String itemCode;
    private boolean equip;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public boolean isEquip() {
        return equip;
    }

    public void setEquip(boolean equip) {
        this.equip = equip;
    }
}