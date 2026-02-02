package com.tearsdeepmind.service;

import com.tearsdeepmind.dto.SubscriberDto;
import com.tearsdeepmind.entity.SubscriberEntity;
import com.tearsdeepmind.repository.SubscriberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    public SubscriberService(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    public List<SubscriberDto> getAllSubscribers() {
        return subscriberRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<SubscriberDto> getSubscriber(String email) {
        return subscriberRepository.findById(email).map(this::convertToDto);
    }

    @Transactional
    public SubscriberDto createSubscriber(SubscriberDto dto) {
        SubscriberEntity entity = new SubscriberEntity(dto.email(), dto.name(), dto.isActive());
        SubscriberEntity saved = subscriberRepository.save(entity);
        return convertToDto(saved);
    }

    @Transactional
    public Optional<SubscriberDto> updateStatus(String email, boolean isActive) {
        return subscriberRepository.findById(email)
                .map(entity -> {
                    entity.setActive(isActive);
                    return convertToDto(subscriberRepository.save(entity));
                });
    }

    @Transactional
    public void deleteSubscriber(String email) {
        subscriberRepository.deleteById(email);
    }

    private SubscriberDto convertToDto(SubscriberEntity entity) {
        return new SubscriberDto(entity.getEmail(), entity.getName(), entity.isActive());
    }
}
