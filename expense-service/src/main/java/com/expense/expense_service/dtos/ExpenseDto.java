package com.expense.expense_service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExpenseDto {

    @JsonProperty("external_id")
    private String externalId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("merchant")
    private String merchant;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("created_at")
    private Timestamp createdAt;
}