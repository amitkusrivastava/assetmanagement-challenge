package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountCreditException;
import com.db.awmd.challenge.exception.AccountDebitException;
import com.db.awmd.challenge.exception.AccountNotPresentException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public void createAccount(Account account) throws DuplicateAccountIdException {
        Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
        if (previousAccount != null) {
            throw new DuplicateAccountIdException(
                    "Account id " + account.getAccountId() + " already exists!");
        }
    }

    @Override
    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    @Override
    public void clearAccounts() {
        accounts.clear();
    }

    @Override
    public Account debitAmount(String fromAccountId, BigDecimal amount) {
        Account account = accounts.computeIfPresent(fromAccountId, (key, acc) -> {
            final BigDecimal newBalance = acc.getBalance().subtract(amount);

            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new AccountDebitException(
                        String.format("After debit, account will have balance of %s. Account cannot have negative balance",
                                newBalance));
            }

            return new Account(key, newBalance);
        });

        if (account == null) {
            Throwable accountNotPresentException = new AccountNotPresentException(String.format("Account with id %s is not present.", fromAccountId));
            throw new AccountDebitException(String.format("Account with id %s is not present.", fromAccountId), accountNotPresentException);
        }

        return account;
    }

    @Override
    public Account creditAmount(String toAccountId, BigDecimal amount) {
        Account account = accounts.computeIfPresent(toAccountId, (key, acc) -> {
            final BigDecimal newBalance = acc.getBalance().add(amount);
            return new Account(key, newBalance);
        });

        if (account == null) {
            Throwable accountNotPresentException = new AccountNotPresentException(String.format("Account with id %s is not present.", toAccountId));
            throw new AccountCreditException(String.format("Exception while crediting the account with key %s", toAccountId), accountNotPresentException);
        }

        return account;
    }
}
