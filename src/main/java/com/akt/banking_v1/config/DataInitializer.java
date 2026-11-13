package com.akt.banking_v1.config;

import com.akt.banking_v1.enums.AccountStatus;
import com.akt.banking_v1.enums.AccountType;
import com.akt.banking_v1.enums.Currency;
import com.akt.banking_v1.enums.Role;
import com.akt.banking_v1.model.Account;
import com.akt.banking_v1.model.Customer;
import com.akt.banking_v1.model.User;
import com.akt.banking_v1.repository.AccountRepository;
import com.akt.banking_v1.repository.CustomerRepository;
import com.akt.banking_v1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public static final String SYSTEM_TRY_ACCOUNT = "TR000000000000000000000001";

    @Value("${app.seed-demo-users:true}")
    private boolean seedDemoUsers;

    @Override
    public void run(String... args) throws Exception {

        if (!customerRepository.existsByIdentityNumber("00000000000")) {
            Customer customer = Customer.builder()
                    .identityNumber("00000000000")
                    .firstName("SYSTEM")
                    .lastName("BANK_VAULT")
                    .email("system.vault@corebank.com")
                    .build();

            customerRepository.save(customer);

            Account account = Account.builder()
                    .accountNumber(SYSTEM_TRY_ACCOUNT)
                    .currency(Currency.TRY)
                    .balance(new BigDecimal("1000000000.0000"))
                    .status(AccountStatus.ACTIVE)
                    .accountType(AccountType.CHECKING)
                    .customer(customer)
                    .build();

            accountRepository.save(account);

            log.info("System vault customer and TRY cash account successfully initialized");
        }

        if (seedDemoUsers && !userRepository.existsByUsername("system_admin")) {

            User admin = User.builder()
                    .username("system_admin")
                    .password(passwordEncoder.encode("system_admin"))
                    .role(Role.SYSTEM)
                    .build();
            userRepository.save(admin);

            User teller = User.builder()
                    .username("teller_user")
                    .password(passwordEncoder.encode("teller_user"))
                    .role(Role.TELLER)
                    .build();
            userRepository.save(teller);

            User user = User.builder()
                    .username("customer_user")
                    .password(passwordEncoder.encode("customer_user"))
                    .role(Role.CUSTOMER)
                    .build();
            userRepository.save(user);

            log.info("Default system, teller, and customer users successfully seeded.");
        }
    }
}
