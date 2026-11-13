package com.plmforge.crm.service;

import com.plmforge.crm.dto.response.PotentialCustomerResponse;
import com.plmforge.crm.repository.PotentialCustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeadService {

    private final PotentialCustomerRepository potentialCustomerRepository;

    public LeadService(PotentialCustomerRepository potentialCustomerRepository) {
        this.potentialCustomerRepository = potentialCustomerRepository;
    }

    @Transactional(readOnly = true)
    public List<PotentialCustomerResponse> getAllLeads() {
        return potentialCustomerRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PotentialCustomerResponse::from)
                .toList();
    }
}
