package com.ranked4.shop.shop_service.repository;

import com.ranked4.shop.shop_service.model.Lootbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LootboxRepository extends JpaRepository<Lootbox, Long> {

}
