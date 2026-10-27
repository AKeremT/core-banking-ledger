package com.akt.banking_v1.model;

import com.akt.banking_v1.enums.AccountStatus;
import com.akt.banking_v1.enums.AccountType;
import com.akt.banking_v1.enums.Currency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String accountNumber;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Currency currency;

    @Column(precision = 19, scale = 4, nullable = false, check = @CheckConstraint(constraint = "balance >= 0"))
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "account")
    @Builder.Default
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;
    @Column(nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account that)) return false;
        return accountNumber != null && accountNumber.equals(that.getAccountNumber());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
