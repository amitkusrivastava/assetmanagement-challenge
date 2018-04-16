package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountCreditException;
import com.db.awmd.challenge.exception.AccountDebitException;
import com.db.awmd.challenge.exception.AccountNotPresentException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.model.TransferCommand;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

    private final AccountsService accountsService;

    private final TransferService transferService;

    @Autowired
    public AccountsController(final AccountsService accountsService,
                              final TransferService transferService) {
        this.accountsService = accountsService;
        this.transferService = transferService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
        log.info("Creating account {}", account);

        try {
            this.accountsService.createAccount(account);
        } catch (DuplicateAccountIdException daie) {
            return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping(path = "/{accountId}")
    public Account getAccount(@PathVariable String accountId) {
        log.info("Retrieving account for id {}", accountId);
        return this.accountsService.getAccount(accountId);
    }

    @PutMapping(path = "/transfer")
    public ResponseEntity<Object> transferAmount(@RequestBody @Valid TransferCommand transferCommand) {

        try {
            boolean requestStatus = transferService.transferAmount(transferCommand);
            log.info("Amount transfer status : " + requestStatus);
        } catch (AccountCreditException | AccountDebitException ae) {
            if (AccountNotPresentException.class.isInstance(ae.getCause())) {
                return new ResponseEntity<>(ae.getCause().getMessage(), HttpStatus.BAD_REQUEST);
            }

            return new ResponseEntity<>(ae.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
