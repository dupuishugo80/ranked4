package com.ranked4.userprofile.userprofile_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ranked4.userprofile.userprofile_service.model.DiscCustomization;

public interface DiscCustomizationRepository extends JpaRepository<DiscCustomization, Long> {
      Optional<DiscCustomization> findByItemCode(String itemCode);
}