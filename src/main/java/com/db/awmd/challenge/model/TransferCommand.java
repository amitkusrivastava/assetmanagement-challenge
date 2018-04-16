package com.db.awmd.challenge.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class TransferCommand {

    @NotNull
    @NotEmpty
    private String fromAccountId;

    @NotNull
    @NotEmpty
    private String toAccountId;

    @NotNull
    @Min(value = 0, message = "Amount to be transfered should be greater than zero.")
    private BigDecimal amount;

    @JsonCreator
    public TransferCommand(@JsonProperty("fromAccountId") final String fromAccountId,
                           @JsonProperty("toAccountId") final String toAccountId,
                           @JsonProperty("amount") final BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }
}
