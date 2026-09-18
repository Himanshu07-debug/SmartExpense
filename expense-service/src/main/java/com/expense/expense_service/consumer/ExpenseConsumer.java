package com.expense.expense_service.consumer;

import com.expense.expense_service.dtos.ExpenseDto;
import com.expense.expense_service.services.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ExpenseConsumer {

    @Autowired
    private ExpenseService expenseService;

    @KafkaListener(topics = "EXPENSE_EVENTS", groupId = "expense-service-group")
    public void consumeExpenseEvent(ExpenseDto event) {
        System.out.println("===> [ExpenseService] Event Received from Kafka for Merchant: " + event.getMerchant());

        // Strict Validation
        if (event.getUserId() == null) {
            System.err.println("!!! [VALIDATION FAILED] Dropping message: user_id cannot be null!");
            // In full production, this gets forwarded to DLT (Dead Letter Topic)
            return;
        }

        if (event.getAmount() == null) {
            System.err.println("!!! [VALIDATION FAILED] Dropping message: amount is missing!");
            return;
        }

        expenseService.createExpense(event);
        System.out.println("===> [ExpenseService] Expense persisted cleanly for UserId: " + event.getUserId());
    }
}