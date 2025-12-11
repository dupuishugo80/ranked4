package com.ranked4.shop.shop_service.DTO;

import java.time.Instant;

public record RecentDropDTO(
        String displayName,
        String userId,
        String rewardItemCode,
        String rewardItemType,
        Integer rewardGoldAmount,
        Instant openedAt) {
}
