package com.plmforge.crm.repository;

import com.plmforge.crm.entity.PotentialCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PotentialCustomerRepository extends JpaRepository<PotentialCustomer, Long> {

    Optional<PotentialCustomer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<PotentialCustomer> findAllByOrderByCreatedAtDesc();
}
