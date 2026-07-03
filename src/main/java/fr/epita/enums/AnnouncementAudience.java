package fr.epita.enums;

public enum AnnouncementAudience {
    ALL_COHORT_STUDENTS,        // Lecturer or Admin: everyone in a specific cohort
    SPECIFIC_STUDENTS,          // Lecturer or Admin: hand-picked students by ID
    ALL_UNIVERSITY_STUDENTS,    // Admin only: every student in the university
    ALL_UNIVERSITY_LECTURERS,   // Admin only: every lecturer in the university
    SPECIFIC_LECTURERS          // Admin only: hand-picked lecturers by ID
}
