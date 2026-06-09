package fr.epita.enums;

public enum CohortStatus {
    NOT_STARTED,
    ONGOING,
    COMPLETED,
    GRADUATED,
    ARCHIVED
}

/**
 NOT_STARTED  Cohort created but start date not reached yet
 ONGOING      Teaching cycles are currently in progress
 COMPLETED    All courses finished
 GRADUATED    Students have completed everything
 ARCHIVED     Cohort hidden from dashboard but kept in DB
 */
