package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountCreditException;
import com.db.awmd.challenge.exception.AccountDebitException;
import com.db.awmd.challenge.model.TransferCommand;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;

@Slf4j
@Service
public class TransferService {

    private final NotificationService notificationService;

    private final AccountsRepository accountsRepository;

    @Autowired
    public TransferService(final NotificationService notificationService,
                           final AccountsRepository accountsRepository) {
        this.notificationService = notificationService;
        this.accountsRepository = accountsRepository;
    }

    public boolean transferAmount(final TransferCommand transferCommand) {
        log.info("Initiating transfer from {} to {} of amount {}", transferCommand.getFromAccountId(),
                transferCommand.getToAccountId(),
                transferCommand.getAmount());

        try {
            final Account debitedAccount = accountsRepository.debitAmount(transferCommand.getFromAccountId(), transferCommand.getAmount());
            notificationService.notifyAboutTransfer(debitedAccount, String.format("Account successfully debited by amount %s",
                    NumberFormat.getCurrencyInstance().format(transferCommand.getAmount())));

            final Account creditedAccount = accountsRepository.creditAmount(transferCommand.getToAccountId(), transferCommand.getAmount());
            notificationService.notifyAboutTransfer(creditedAccount, String.format("Account successfully credited by amount %s",
                    NumberFormat.getCurrencyInstance().format(transferCommand.getAmount())));

        } catch (AccountDebitException ade) {
            log.error("Exception while debiting the account.", ade);
            throw ade;
        } catch (AccountCreditException ace) {
            log.error("Exception while debiting the account.", ace);
            Account creditedAccount = accountsRepository.creditAmount(transferCommand.getFromAccountId(), transferCommand.getAmount());
            notificationService.notifyAboutTransfer(creditedAccount, String.format("Account successfully credited by amount %s",
                    NumberFormat.getCurrencyInstance().format(transferCommand.getAmount())));
            throw ace;
        }

        return true;
    }
}
