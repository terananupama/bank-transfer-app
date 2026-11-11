package com.example.bankapp.exception;

public class SimulatedFailureException extends BankException {
    public SimulatedFailureException(String message) {
        super(message);
    }
}