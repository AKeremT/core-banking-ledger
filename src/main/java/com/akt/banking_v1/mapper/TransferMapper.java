package com.akt.banking_v1.mapper;

import com.akt.banking_v1.dto.TransferResponse;
import com.akt.banking_v1.enums.EntryType;
import com.akt.banking_v1.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    public TransferResponse toResponse(Transaction transaction) {

        String senderAccount = transaction.getLedgerEntries().stream()
                .filter(entry -> entry.getEntryType() == EntryType.DEBIT)
                .map(entry -> entry.getAccount().getAccountNumber())
                .findFirst()
                .orElse(null);

        String receiverAccount = transaction.getLedgerEntries().stream()
                .filter(entry -> entry.getEntryType() == EntryType.CREDIT)
                .map(entry -> entry.getAccount().getAccountNumber())
                .findFirst()
                .orElse(null);

        return new TransferResponse(
                transaction.getId(),
                transaction.getReferenceNumber(),
                transaction.getStatus(),
                senderAccount,
                receiverAccount,
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getCreatedAt()
        );
    }
}
