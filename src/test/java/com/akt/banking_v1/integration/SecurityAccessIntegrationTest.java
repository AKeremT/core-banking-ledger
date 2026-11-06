package com.akt.banking_v1.integration;

import com.akt.banking_v1.TestcontainersConfiguration;
import com.akt.banking_v1.dto.CreateCustomerRequest;
import com.akt.banking_v1.dto.DepositRequest;
import com.akt.banking_v1.dto.TransferRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class SecurityAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return 401 Unauthorized when unauthenticated request accesses secured endpoint.")
    void givenNoAuthentication_whenAccessSecuredEndpoint_thenReturns401Unauthorized() throws Exception {

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when CUSTOMER role attempts to create customer")
    @WithMockUser(roles = "CUSTOMER")
    void givenCustomerRole_whenCreateCustomer_thenReturns403Forbidden() throws Exception {

        CreateCustomerRequest createCustomerRequest = new CreateCustomerRequest(
                "21742013370",
                "Kerem",
                "Temel",
                "akerem1427@gmail.com"
        );

        String content = objectMapper.writeValueAsString(createCustomerRequest);

        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 201 Created when TELLER role creates customer")
    @WithMockUser(roles = "TELLER")
    void givenTellerRole_whenCreateCustomer_thenReturns201Created() throws Exception {

        CreateCustomerRequest createCustomerRequest = new CreateCustomerRequest(
                "98765432109",
                "Kerem",
                "Temel",
                "akerem1427@gmail.com"
        );

        String content = objectMapper.writeValueAsString(createCustomerRequest);

        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return 200 OK when unauthenticated request accesses public health endpoint")
    void givenNoAuthentication_whenAccessPublicHealthEndpoint_thenReturns200OK() throws Exception {

        mockMvc.perform(get("/actuator/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when TELLER role attempts to execute transfer")
    @WithMockUser(roles = "TELLER")
    void givenTellerRole_whenExecuteTransfer_thenReturns403Forbidden() throws Exception {

        TransferRequest transferRequest = new TransferRequest(
                "111",
                "222",
                BigDecimal.valueOf(100.00),
                "kira"
        );

        String content = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(content))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when CUSTOMER role attempts to deposit cash")
    @WithMockUser(roles = "CUSTOMER")
    void givenCustomerRole_whenDepositCash_thenReturns403Forbidden() throws Exception {

        DepositRequest depositRequest = new DepositRequest(
                "12345",
                BigDecimal.valueOf(100.00),
                "kira"
        );
        String content = objectMapper.writeValueAsString(depositRequest);

        mockMvc.perform(post("/api/v1/transfers/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .content(content))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when CUSTOMER role accesses system metrics")
    @WithMockUser(roles = "CUSTOMER")
    void givenCustomerRole_whenAccessMetrics_thenReturns403Forbidden() throws Exception {

        mockMvc.perform(get("/actuator/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 200 OK when SYSTEM role accesses system metrics")
    @WithMockUser(roles = "SYSTEM")
    void givenSystemRole_whenAccessMetrics_thenReturns200OK() throws Exception {

        mockMvc.perform(get("/actuator/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
