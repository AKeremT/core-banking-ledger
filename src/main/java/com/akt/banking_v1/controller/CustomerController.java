package com.akt.banking_v1.controller;

import com.akt.banking_v1.dto.CreateCustomerRequest;
import com.akt.banking_v1.dto.CustomerResponse;
import com.akt.banking_v1.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer onboarding and identity lookup endpoints")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Onboard / register a new customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Identity number or email already exists")
    })
    @PostMapping
    @PreAuthorize("hasRole('TELLER')")
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody @Valid CreateCustomerRequest request){

        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @Operation(summary = "Get customer by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER')")
    public ResponseEntity<CustomerResponse> getCustomer(
            @Parameter(description = "Customer ID", example = "1")
            @PathVariable Long id){

        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @Operation(summary = "Get customer by Identity Number (TCKN / VKN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/identity/{identityNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER')")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @Parameter(description = "11-digit Turkish National ID or Tax ID", example = "12345678901")
            @PathVariable("identityNumber") String identityNumber){

        return ResponseEntity.ok(customerService.getCustomerByIdentityNumber(identityNumber));
    }
}
