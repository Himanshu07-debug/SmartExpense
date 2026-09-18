package com.expense.expense_service.controllers;

import com.expense.expense_service.dtos.ExpenseDto;
import com.expense.expense_service.services.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expense/v1")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    // 1. Create Expense manually
    @PostMapping
    public ResponseEntity<ExpenseDto> createExpense(@RequestBody ExpenseDto dto) {
        return ResponseEntity.ok(expenseService.createExpense(dto));
    }

    // 2. Fetch all expenses of a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ExpenseDto>> getExpensesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(expenseService.getExpensesByUserId(userId));
    }

    // 3. Update an expense
    @PutMapping("/{externalId}/user/{userId}")
    public ResponseEntity<ExpenseDto> updateExpense(
            @PathVariable String externalId,
            @PathVariable Long userId,
            @RequestBody ExpenseDto dto) {
        return ResponseEntity.ok(expenseService.updateExpense(externalId, userId, dto));
    }
}