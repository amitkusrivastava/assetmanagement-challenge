package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountCreditException;
import com.db.awmd.challenge.exception.AccountDebitException;
import com.db.awmd.challenge.exception.AccountNotPresentException;
import org.assertj.core.api.Assertions;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AccountsRepositoryInMemoryTest {
    private static final String ACCOUNT_ID = "ID-123";

    @InjectMocks
    private AccountsRepositoryInMemory repo;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        repo.clearAccounts();
    }

    @Test
    public void debitAmount_failsOnAbsentFromAccount() {
        expectedException.expect(AccountDebitException.class);
        expectedException.expectCause(IsInstanceOf.<Throwable>instanceOf(AccountNotPresentException.class));

        final Account account = repo.debitAmount(ACCOUNT_ID, BigDecimal.valueOf(100));
        assertThat(account).isNull();
    }

    @Test
    public void debitAmount_failsDueToNegativeBalance() {
        repo.createAccount(new Account(ACCOUNT_ID, BigDecimal.valueOf(500)));
        Account existingAccount = repo.getAccount(ACCOUNT_ID);

        expectedException.expect(AccountDebitException.class);
        expectedException.expectMessage("Account cannot have negative balance");

        Account account = repo.debitAmount(ACCOUNT_ID, BigDecimal.valueOf(1000));
        assertThat(account).isNull();
        assertThat(existingAccount.getBalance()).isEqualByComparingTo("500");
    }

    @Test
    public void debitAmount_success() {
        repo.createAccount(new Account(ACCOUNT_ID, BigDecimal.valueOf(1000)));

        Account account = repo.debitAmount(ACCOUNT_ID, BigDecimal.valueOf(500));
        assertThat(account).isNotNull();
        assertThat(account.getBalance()).isEqualByComparingTo("500");

        Account accountFromMap = repo.getAccount(ACCOUNT_ID);
        assertThat(accountFromMap.getBalance()).isEqualByComparingTo("500.0");
    }

    @Test
    public void creditAmount_failsDueToAbsentToAccount() {
        expectedException.expect(AccountCreditException.class);
        expectedException.expectCause(IsInstanceOf.<Throwable>instanceOf(AccountNotPresentException.class));

        Account account = repo.creditAmount(ACCOUNT_ID, BigDecimal.valueOf(1000));
        assertThat(account).isNull();
    }

    @Test
    public void creditAmount_success() {
        repo.createAccount(new Account(ACCOUNT_ID, BigDecimal.valueOf(1000)));

        Account account = repo.creditAmount(ACCOUNT_ID, BigDecimal.valueOf(500));
        assertThat(account).isNotNull();
        assertThat(account.getBalance()).isEqualByComparingTo("1500");

        Account accountFromMap = repo.getAccount(ACCOUNT_ID);
        assertThat(accountFromMap.getBalance()).isEqualByComparingTo("1500.0");
    }
}