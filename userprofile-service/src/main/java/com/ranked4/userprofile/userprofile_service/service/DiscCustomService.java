package com.ranked4.userprofile.userprofile_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ranked4.userprofile.userprofile_service.dto.DiscCustomizationDTO;
import com.ranked4.userprofile.userprofile_service.model.DiscCustomization;
import com.ranked4.userprofile.userprofile_service.repository.DiscCustomizationRepository;

@Service
public class DiscCustomService {

    private final DiscCustomizationRepository discCustomizationRepository;

    public DiscCustomService(DiscCustomizationRepository discCustomizationRepository) {
        this.discCustomizationRepository = discCustomizationRepository;
    }

    @Transactional
    public DiscCustomizationDTO createDiscCustomization(DiscCustomizationDTO dto) {
        discCustomizationRepository.findByItemCode(dto.getItemCode())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("DiscCustomization with this itemCode already exists");
                });

        DiscCustomization entity = new DiscCustomization();
        entity.setItemCode(dto.getItemCode());
        entity.setDisplayName(dto.getDisplayName());
        entity.setType(dto.getType());
        entity.setValue(dto.getValue());

        DiscCustomization saved = discCustomizationRepository.save(entity);
        return new DiscCustomizationDTO(saved);
    }
    
}
