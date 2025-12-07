package com.ranked4.shop.shop_service.repository;

import com.ranked4.shop.shop_service.model.LootboxOpening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LootboxOpeningRepository extends JpaRepository<LootboxOpening, Long> {

}
