package com.expense.expense_service.services;

import com.expense.expense_service.dtos.ExpenseDto;
import com.expense.expense_service.entities.Expense;
import com.expense.expense_service.repositories.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public ExpenseDto createExpense(ExpenseDto dto) {
        Expense expense = Expense.builder()
                .userId(dto.getUserId())
                .amount(dto.getAmount())
                .merchant(dto.getMerchant())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .build();

        Expense saved = expenseRepository.save(expense);
        return mapToDto(saved);
    }

    public List<ExpenseDto> getExpensesByUserId(Long userId) {
        return expenseRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ExpenseDto updateExpense(String externalId, Long userId, ExpenseDto dto) {
        Expense expense = expenseRepository.findByExternalIdAndUserId(externalId, userId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (dto.getAmount() != null) expense.setAmount(dto.getAmount());
        if (dto.getMerchant() != null) expense.setMerchant(dto.getMerchant());
        if (dto.getCurrency() != null) expense.setCurrency(dto.getCurrency());

        Expense updated = expenseRepository.save(expense);
        return mapToDto(updated);
    }

    private ExpenseDto mapToDto(Expense expense) {
        return ExpenseDto.builder()
                .externalId(expense.getExternalId())
                .userId(expense.getUserId())
                .amount(expense.getAmount())
                .merchant(expense.getMerchant())
                .currency(expense.getCurrency())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}