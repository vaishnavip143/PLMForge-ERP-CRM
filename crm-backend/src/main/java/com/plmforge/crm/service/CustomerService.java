package com.plmforge.crm.service;

import com.plmforge.crm.dto.response.CustomerResponse;
import com.plmforge.crm.entity.Customer;
import com.plmforge.crm.exception.ResourceNotFoundException;
import com.plmforge.crm.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getMyProfile(String email) {
        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No customer profile found for email: " + email));
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }
}
