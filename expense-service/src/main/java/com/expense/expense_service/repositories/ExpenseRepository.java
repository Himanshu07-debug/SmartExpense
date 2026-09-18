package com.expense.expense_service.repositories;

import com.expense.expense_service.entities.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserId(Long userId);
    List<Expense> findByUserIdAndCreatedAtBetween(Long userId, Timestamp start, Timestamp end);
    Optional<Expense> findByExternalIdAndUserId(String externalId, Long userId);
}