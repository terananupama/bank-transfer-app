package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("allCustomers", bankService.getAllCustomers(null));
        model.addAttribute("customers", bankService.getAllCustomers(keyword));
        model.addAttribute("totalCustomers", bankService.getTotalCustomersCount());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.getSuccessCount());
        model.addAttribute("failedCount", bankService.getFailedCount());
        model.addAttribute("transactions", bankService.getLatestTransactions());
        model.addAttribute("keyword", keyword);

        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
        return "bank";
    }

    @PostMapping("/transfer")
    public String handleTransfer(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.transferRequest", result);
            redirectAttributes.addFlashAttribute("transferRequest", request);
            return "redirect:/";
        }

        try {
            bankService.transfer(request.getFromAccount(), request.getToAccount(), request.getAmount(), request.isSimulateFailure());
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully!");
        } catch (Exception e) {
            auditService.recordFailure(request.getFromAccount(), request.getToAccount(), request.getAmount(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Transfer failed: " + e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/customers")
    public String handleAddCustomer(@Valid @ModelAttribute("customer") Customer customer,
                                     BindingResult result,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customer", result);
            redirectAttributes.addFlashAttribute("customer", customer);
            return "redirect:/";
        }

        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("success", "Customer saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model) {
        Customer customer = bankService.getCustomerByAccount(accountNumber);
        model.addAttribute("customer", customer);
        model.addAttribute("transactions", bankService.getAccountHistory(accountNumber));
        model.addAttribute("totalSent", bankService.getTotalSentByAccount(accountNumber));
        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers", bankService.getLargeTransfers(min));
        return "large-transfers";
    }

    @GetMapping("/deposit")
    public String showDepositForm(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers(null));
        return "deposit";
    }

    @PostMapping("/deposit")
    public String handleDeposit(@RequestParam String accountNumber,
                                @RequestParam BigDecimal amount,
                                RedirectAttributes redirectAttributes) {
        try {
            bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Deposited LKR " + amount + " successfully to " + accountNumber);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Deposit failed: " + e.getMessage());
        }
        return "redirect:/deposit";
    }

    @GetMapping("/withdraw")
    public String showWithdrawForm(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers(null));
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String handleWithdraw(@RequestParam String accountNumber,
                                 @RequestParam BigDecimal amount,
                                 RedirectAttributes redirectAttributes) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Withdrew LKR " + amount + " successfully from " + accountNumber);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Withdrawal failed: " + e.getMessage());
        }
        return "redirect:/withdraw";
    }
}