package fr.epita.enums;

public enum StudentStatus {
    ACTIVE,
    INACTIVE,
    PAYMENT_PENDING,
    PROBATION,
    SUSPENDED,
    EXPELLED,
    COMPLETED,
    GRADUATED,
    DROPPED_OUT
}

/**
 ACTIVE — student is fully enrolled and studying
 INACTIVE — student is temporarily not studying.. take a gap
 PAYMENT_PENDING — fees not paid, portal access may be restricted
 PROBATION — academic warning
 SUSPENDED — temporarily suspended
 EXPELLED — permanently removed
 COMPLETED — finished all modules but not yet graduate
 GRADUATED — officially graduated
 DROPPED_OUT — student left voluntarily
 **/
