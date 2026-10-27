package com.akt.banking_v1.repository;

import com.akt.banking_v1.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdentityNumber(String identityNumber);
    Optional<Customer> findByEmail(String email);
    boolean existsByIdentityNumber(String identityNumber);
    boolean existsByEmail(String email);
}
