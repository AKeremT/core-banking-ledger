package com.akt.banking_v1.mapper;

import com.akt.banking_v1.dto.CustomerResponse;
import com.akt.banking_v1.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {

        return new CustomerResponse(
                customer.getId(),
                customer.getIdentityNumber(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getCreatedAt()
        );
    }
}
