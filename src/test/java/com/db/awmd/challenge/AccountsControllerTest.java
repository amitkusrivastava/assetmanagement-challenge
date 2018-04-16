package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    public void createAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        Account account = accountsService.getAccount("Id-123");
        assertThat(account.getAccountId()).isEqualTo("Id-123");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    public void createDuplicateAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNegativeBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountEmptyAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void getAccount() throws Exception {
        String uniqueAccountId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
        this.accountsService.createAccount(account);
        this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
    }

    @Test
    public void transferAmount_failsDueToEmptyFromAccount() throws Exception {
        mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"\", \"toAccountId\": \"Id-234\", \"amount\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void transferAmount_failsDueToEmptyToAccount() throws Exception {
        mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"Id-123\", \"toAccountId\": \"\", \"amount\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void transferAmount_failsDueToNegativeAmount() throws Exception {
        mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"Id-123\", \"toAccountId\": \"Id-234\", \"amount\":-1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void transferAmount_failsDueToAbsentFromAccount() throws Exception {
        mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"Id-123\", \"toAccountId\": \"Id-234\", \"amount\":1000}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Account with id Id-123 is not present."));
    }

    @Test
    public void transferAmount_failsDueToAbsentToAccount() throws Exception {

        accountsService.createAccount(new Account("Id-123", BigDecimal.valueOf(1000.00)));

        mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"Id-123\", \"toAccountId\": \"Id-234\", \"amount\":1000}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Account with id Id-234 is not present."));
    }

    @Test
    public void transferAmount_failsDueToNegativeBalanceAfterDebit() throws Exception {

        accountsService.createAccount(new Account("Id-123", BigDecimal.valueOf(1000.00)));
        accountsService.createAccount(new Account("Id-234", BigDecimal.valueOf(1000.00)));

        mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"Id-123\", \"toAccountId\": \"Id-234\", \"amount\":2000}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("After debit, account will have balance of -1000.0. Account cannot have negative balance"));

        Account fromAccount = accountsService.getAccount("Id-123");
        assertThat(fromAccount.getBalance()).isEqualByComparingTo("1000");

        Account toAccount = accountsService.getAccount("Id-234");
        assertThat(toAccount.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    public void transferAmount_success() throws Exception {

        accountsService.createAccount(new Account("Id-456", BigDecimal.valueOf(1000.00)));
        accountsService.createAccount(new Account("Id-678", BigDecimal.valueOf(1000.00)));

        mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"Id-456\", \"toAccountId\": \"Id-678\", \"amount\":500}"))
                .andExpect(status().isOk());

        Account fromAccount = accountsService.getAccount("Id-456");
        assertThat(fromAccount.getBalance()).isEqualByComparingTo("500");

        Account toAccount = accountsService.getAccount("Id-678");
        assertThat(toAccount.getBalance()).isEqualByComparingTo("1500");
    }
}
