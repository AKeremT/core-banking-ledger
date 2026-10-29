package com.akt.banking_v1.service;

import com.akt.banking_v1.common.exception.CustomerAlreadyExistsException;
import com.akt.banking_v1.common.exception.CustomerNotFoundException;
import com.akt.banking_v1.dto.CreateCustomerRequest;
import com.akt.banking_v1.dto.CustomerResponse;
import com.akt.banking_v1.mapper.CustomerMapper;
import com.akt.banking_v1.model.Customer;
import com.akt.banking_v1.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional(rollbackFor = Exception.class)
    public CustomerResponse createCustomer(CreateCustomerRequest request) {

        if (customerRepository.existsByIdentityNumber(request.identityNumber()))
            throw new CustomerAlreadyExistsException("Customer already exists with identity number: " + request.identityNumber());

        if (customerRepository.existsByEmail(request.email()))
            throw new CustomerAlreadyExistsException("Customer already exists with email: " + request.email());

        Customer customer = Customer.builder()
                .identityNumber(request.identityNumber())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return customerMapper.toResponse(savedCustomer);
    }

    public CustomerResponse getCustomerById(Long id) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
        return customerMapper.toResponse(customer);
    }

    public CustomerResponse getCustomerByIdentityNumber(String identityNumber) {

        Customer customer = customerRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with identity number: " + identityNumber));
        return customerMapper.toResponse(customer);
    }


}
