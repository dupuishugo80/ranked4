package com.ranked4.userprofile.userprofile_service.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;

import com.ranked4.userprofile.userprofile_service.dto.DiscCustomizationDTO;
import com.ranked4.userprofile.userprofile_service.model.DiscCustomization;
import com.ranked4.userprofile.userprofile_service.repository.DiscCustomizationRepository;

@Service
public class DiscCustomService {

    private final DiscCustomizationRepository discCustomizationRepository;

    public DiscCustomService(DiscCustomizationRepository discCustomizationRepository) {
        this.discCustomizationRepository = discCustomizationRepository;
    }

    public Page<DiscCustomizationDTO> getAllDiscCustomizations(Pageable pageable) {
        Page<DiscCustomization> entities = discCustomizationRepository.findAll(pageable);
        return entities.map(DiscCustomizationDTO::new);
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
