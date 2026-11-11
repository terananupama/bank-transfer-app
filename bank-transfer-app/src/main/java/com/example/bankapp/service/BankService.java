package com.example.bankapp.service;

import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.SimulatedFailureException;
import com.example.bankapp.model.*;
import com.example.bankapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    public static final String BANK_FEE_ACCOUNT = "999999";

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {
        if (fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver accounts must be different");
        }

        Customer sender = customerRepository.getByAccountNumber(fromAccount)
                .orElseThrow(() -> new BankException("Sender account not found: " + fromAccount));
        
        Customer receiver = customerRepository.getByAccountNumber(toAccount)
                .orElseThrow(() -> new BankException("Receiver account not found: " + toAccount));

        // Task 12: 1% transfer fee
        BigDecimal fee = amount.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDeduction = amount.add(fee);

        // Step 1: Debit sender
        int rowsUpdated = customerRepository.debit(fromAccount, totalDeduction);
        if (rowsUpdated == 0) {
            throw new BankException("Insufficient balance in account " + fromAccount + " (Required: LKR " + totalDeduction + " including 1% fee)");
        }

        // Step 2: Test Simulation Check
        // Step 2: Crash Simulation Check (Rollback Test)
        if (simulateFailure) {
    throw new SimulatedFailureException("Simulated system failure after the withdrawal");
}

        // Step 3: Credit receiver
        customerRepository.credit(toAccount, amount);

        // Step 4: Credit 1% fee to Bank Fee Account (999999)
        customerRepository.credit(BANK_FEE_ACCOUNT, fee);

        // Step 5: Save Transaction
        BankTransaction tx = new BankTransaction();
        tx.setFromAccount(fromAccount);
        tx.setToAccount(toAccount);
        tx.setAmount(amount);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setDescription("Transfer completed (Fee: LKR " + fee + ")");
        tx.setCreatedAt(LocalDateTime.now());

        return transactionRepository.save(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deposit(String accountNumber, BigDecimal amount) {
        Customer customer = customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankException("Account not found: " + accountNumber));

        customerRepository.credit(accountNumber, amount);

        BankTransaction tx = new BankTransaction();
        tx.setFromAccount(null);
        tx.setToAccount(accountNumber);
        tx.setAmount(amount);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setDescription("Direct Cash Deposit");
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String accountNumber, BigDecimal amount) {
        Customer customer = customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankException("Account not found: " + accountNumber));

        int updated = customerRepository.debit(accountNumber, amount);
        if (updated == 0) {
            throw new BankException("Insufficient balance in account: " + accountNumber);
        }

        BankTransaction tx = new BankTransaction();
        tx.setFromAccount(accountNumber);
        tx.setToAccount(null);
        tx.setAmount(amount);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setDescription("Direct Cash Withdrawal");
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    public Customer createCustomer(Customer customer) {
        if (customerRepository.countAccounts(customer.getAccountNumber())) {
            throw new BankException("Account number " + customer.getAccountNumber() + " already exists");
        }
        if (customerRepository.countEmails(customer.getEmail())) {
            throw new BankException("Email " + customer.getEmail() + " already exists");
        }
        return customerRepository.save(customer);
    }

    public Customer getCustomerByAccount(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankException("Account not found: " + accountNumber));
    }

    public List<Customer> getAllCustomers(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return customerRepository.searchByName(keyword);
        }
        return customerRepository.findAllOrdered();
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public BigDecimal getTotalSentByAccount(String accountNumber) {
        return transactionRepository.getTotalSentByAccount(accountNumber);
    }

    public List<BankTransaction> getLargeTransfers(BigDecimal minAmount) {
        return transactionRepository.findLargeTransfers(minAmount);
    }

    public long getSuccessCount() {
        return transactionRepository.countByStatus(TransactionStatus.SUCCESS);
    }

    public long getFailedCount() {
        return transactionRepository.countByStatus(TransactionStatus.FAILED);
    }

    public long getTotalCustomersCount() {
        return customerRepository.countCustomersNative();
    }
}