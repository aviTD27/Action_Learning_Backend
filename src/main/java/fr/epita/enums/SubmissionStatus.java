package fr.epita.enums;

/** Lifecycle state of an assignment (submission). */
public enum SubmissionStatus {
    DRAFT,      // not visible to students
    PUBLISHED,  // visible to the assigned cohort's students
    ARCHIVED    // hidden but kept for reference
}
