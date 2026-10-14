package com.fintech.mco1.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Loans are paid in several parts represented by Installments.
 */
@Entity
@Table(name = "installments", uniqueConstraints = @UniqueConstraint(columnNames = {"loan_id", "installmentNumber"}))
public class Installment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private int installmentNumber;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountDue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalPortion;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestPortion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstallmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    public Installment() {}

    public long getId() {
        return id;
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public BigDecimal getPrincipalPortion() {
        return principalPortion;
    }

    public BigDecimal getInterestPortion() {
        return interestPortion;
    }

    public InstallmentStatus getStatus() {
        return status;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setInstallmentNumber(int installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setAmountDue(BigDecimal amountDue) {
        this.amountDue = amountDue;
    }

    public void setPrincipalPortion(BigDecimal principalPortion) {
        this.principalPortion = principalPortion;
    }

    public void setInterestPortion(BigDecimal interestPortion) {
        this.interestPortion = interestPortion;
    }

    public void setStatus(InstallmentStatus status) {
        this.status = status;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }
}
