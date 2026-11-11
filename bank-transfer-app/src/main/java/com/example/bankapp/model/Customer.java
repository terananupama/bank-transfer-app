package com.example.bankapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^\\d{6}$", message = "Account number must be 6 digits")
    @Column(unique = true, nullable = false)
    private String accountNumber;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(unique = true, nullable = false)
    private String email;

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.00", message = "Balance must be non-negative")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal balance;
}