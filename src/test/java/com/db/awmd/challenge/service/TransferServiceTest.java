package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountCreditException;
import com.db.awmd.challenge.exception.AccountDebitException;
import com.db.awmd.challenge.exception.AccountNotPresentException;
import com.db.awmd.challenge.model.TransferCommand;
import com.db.awmd.challenge.repository.AccountsRepository;
import org.assertj.core.api.Assertions;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class TransferServiceTest {

    private static final String fromAccountId = "Id-123";
    private static final String toAccountId = "Id-234";
    private static final BigDecimal amount = BigDecimal.valueOf(300.00);

    @InjectMocks
    private TransferService transferService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AccountsRepository accountsRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void transferAmount_failsOnAbsentFromAccount() {
        Throwable anpe = new AccountNotPresentException(String.format("Account with id %s is not present.", fromAccountId));
        when(accountsRepository.debitAmount(fromAccountId, amount)).thenThrow(new AccountDebitException("Account debit exception", anpe));

        expectedException.expect(AccountDebitException.class);
        expectedException.expectCause(IsInstanceOf.<Throwable>instanceOf(AccountNotPresentException.class));

        final TransferCommand transferCommand = new TransferCommand(fromAccountId, toAccountId, amount);

        transferService.transferAmount(transferCommand);
        verify(accountsRepository, never()).creditAmount(toAccountId, amount);
    }

    @Test
    public void transferAmount_failsDueToNegativeBalance() {
        when(accountsRepository.debitAmount(fromAccountId, amount)).thenThrow(new AccountDebitException("Account cannot have negative balance"));

        expectedException.expect(AccountDebitException.class);
        expectedException.expectMessage("Account cannot have negative balance");

        final TransferCommand transferCommand = new TransferCommand(fromAccountId, toAccountId, amount);

        transferService.transferAmount(transferCommand);
        verify(accountsRepository, never()).creditAmount(toAccountId, amount);
    }

    @Test
    public void transferAmount_failsDueToAbsentToAccount() {
        when(accountsRepository.debitAmount(fromAccountId, amount)).thenReturn(new Account(fromAccountId, amount));

        Throwable anpe = new AccountNotPresentException(String.format("Account with id %s is not present.", toAccountId));
        when(accountsRepository.creditAmount(toAccountId, amount)).thenThrow(new AccountCreditException("Account credit exception", anpe));

        expectedException.expect(AccountCreditException.class);
        expectedException.expectCause(IsInstanceOf.<Throwable>instanceOf(AccountNotPresentException.class));

        final TransferCommand transferCommand = new TransferCommand(fromAccountId, toAccountId, amount);

        transferService.transferAmount(transferCommand);
        verify(accountsRepository).debitAmount(fromAccountId, amount);
        verify(accountsRepository).creditAmount(toAccountId, amount);
        verify(accountsRepository).creditAmount(fromAccountId, amount);
    }

    @Test
    public void transferAmount_success() {
        when(accountsRepository.debitAmount(fromAccountId, amount)).thenReturn(new Account(fromAccountId, amount));
        when(accountsRepository.creditAmount(toAccountId, amount)).thenReturn(new Account(toAccountId, amount));

        final TransferCommand transferCommand = new TransferCommand(fromAccountId, toAccountId, amount);

        boolean status = transferService.transferAmount(transferCommand);
        assertThat(status).isTrue();
        verify(accountsRepository).debitAmount(fromAccountId, amount);
        verify(accountsRepository).creditAmount(toAccountId, amount);
        verify(accountsRepository, never()).creditAmount(fromAccountId, amount);
    }
}