package com.ranked4.shop.shop_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ranked4.shop.shop_service.model.LootboxOpening;

@Repository
public interface LootboxOpeningRepository extends JpaRepository<LootboxOpening, Long> {

    @Query("SELECT lo FROM LootboxOpening lo WHERE lo.lootboxId = :lootboxId ORDER BY lo.openedAt DESC")
    List<LootboxOpening> findTop5ByLootboxIdOrderByOpenedAtDesc(
        @Param("lootboxId") Long lootboxId,
        Pageable pageable
    );

}
