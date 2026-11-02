package com.akt.banking_v1.controller;

import com.akt.banking_v1.dto.CreateCustomerRequest;
import com.akt.banking_v1.dto.CustomerResponse;
import com.akt.banking_v1.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody @Valid CreateCustomerRequest request){

        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id){

        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping("/identity/{identityNumber}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable("identityNumber") String identityNumber){

        return ResponseEntity.ok(customerService.getCustomerByIdentityNumber(identityNumber));
    }
}
