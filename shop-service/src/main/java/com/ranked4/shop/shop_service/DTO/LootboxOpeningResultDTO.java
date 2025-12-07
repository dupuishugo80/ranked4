package com.ranked4.shop.shop_service.DTO;

public record LootboxOpeningResultDTO(
    Long openingId,
    String rewardItemCode,
    String rewardItemType,
    Integer rewardGoldAmount,
    String displayMessage
) {
    public static LootboxOpeningResultDTO create(Long openingId, String itemCode, String itemType, Integer goldAmount) {
        String message = switch (itemType) {
            case "DISC" -> "You received: " + itemCode + "!";
            case "GOLD" -> "You received: " + goldAmount + " gold!";
            default -> "You received a reward!";
        };

        return new LootboxOpeningResultDTO(openingId, itemCode, itemType, goldAmount, message);
    }
}
