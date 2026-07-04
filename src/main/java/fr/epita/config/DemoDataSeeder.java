package fr.epita.config;

import fr.epita.enums.CohortSeason;
import fr.epita.enums.CohortStatus;
import fr.epita.enums.CourseStatus;
import fr.epita.enums.LecturerStatus;
import fr.epita.enums.ProgrammeStatus;
import fr.epita.enums.Role;
import fr.epita.enums.StudentStatus;
import fr.epita.model.*;
import fr.epita.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds rich demo data under the new hierarchy:
 * University → Cohort (intake season) ←M2M→ Programme → Semester → Course → Assignment.
 *
 * <p>Per university: 1 uni-admin, 6 lecturers, 2 master programmes, 2 intakes
 * (Spring 2026 + Fall 2025) with BOTH programmes running in each intake, 3 semesters
 * per programme, 5 courses per semester, and students enrolled per programme + intake.
 *
 * <p>Every demo account uses the password {@value #DEMO_PASSWORD}. Seeding is per-university
 * idempotent (guarded by university code). Disable with {@code app.seed.demo=false}.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "Password@123";
    private static final int STUDENTS_PER_PROGRAMME_PER_INTAKE = 3;

    private final UniversityRepository universityRepository;
    private final ProgrammeRepository programmeRepository;
    private final CohortRepository cohortRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.demo:true}")
    private boolean seedDemo;

    /** Rolling index into STUDENT_NAMES so student names stay unique across universities. */
    private int studentSeq = 0;

    private static final String[][] STUDENT_NAMES = {
            {"John", "Smith"}, {"Aisha", "Khan"}, {"Liam", "Brown"}, {"Sofia", "Rossi"},
            {"Noah", "Wilson"}, {"Emma", "Garcia"}, {"Lucas", "Muller"}, {"Mia", "Chen"},
            {"Paul", "Martin"}, {"Lina", "Haddad"}, {"Marco", "Bianchi"}, {"Yuki", "Tanaka"},
            {"Elena", "Petrov"}, {"Omar", "Farouk"}, {"Nina", "Popescu"}, {"Diego", "Lopez"},
            {"Clara", "Dubois"}, {"Hugo", "Meyer"}, {"Sara", "Nilsson"}, {"Ivan", "Kovac"},
            {"Amara", "Okafor"}, {"Ravi", "Patel"}, {"Leah", "Cohen"}, {"Tom", "Andersen"},
            {"Zoe", "Fischer"}, {"Ken", "Watanabe"}, {"Maya", "Singh"}, {"Ali", "Rahman"},
            {"Nora", "Jensen"}, {"Pablo", "Torres"}, {"Ines", "Costa"}, {"Sven", "Larsson"},
            {"Fatima", "Zahra"}, {"Bruno", "Silva"}, {"Anya", "Volkov"}, {"Ethan", "Wright"},
            {"Priya", "Nair"}, {"Jonas", "Weber"}, {"Carmen", "Ruiz"}, {"Kofi", "Mensah"},
    };

    @Override
    public void run(String... args) {
        if (!seedDemo) {
            log.info("[DemoDataSeeder] app.seed.demo=false — skipping.");
            return;
        }

        seedUniversity("EPITA", "EPITA", "epita.fr", "Alice", "Bernard",
                new String[][]{{"Marie", "Curie"}, {"Alan", "Turing"}, {"Grace", "Hopper"},
                        {"Dennis", "Ritchie"}, {"Ada", "Lovelace"}, {"Linus", "Torvalds"}},
                "MSc Software Engineering", "MSC-SE", new String[]{
                        "Advanced Programming", "Software Architecture", "Databases", "Web Technologies", "Agile Methods",
                        "Distributed Systems", "Cloud Computing", "DevOps & CI/CD", "Security Engineering", "Microservices",
                        "Machine Learning for SE", "Mobile Development", "Software Testing", "Capstone Project", "Research Methods"},
                "MSc Data Science & AI", "MSC-DS", new String[]{
                        "Statistics for Data Science", "Python for Data", "Data Wrangling", "Linear Algebra", "Databases for DS",
                        "Machine Learning", "Deep Learning", "Big Data Systems", "Data Visualization", "Natural Language Processing",
                        "Computer Vision", "Reinforcement Learning", "MLOps", "Capstone Project", "Ethics in AI"});

        seedUniversity("Sorbonne University", "SORB", "sorbonne.fr", "Hugo", "Moreau",
                new String[][]{{"Henri", "Poincare"}, {"Emmy", "Noether"}, {"Carl", "Gauss"},
                        {"Sofia", "Kovalevskaya"}, {"Blaise", "Pascal"}, {"Joseph", "Fourier"}},
                "MSc Computer Science", "MSC-CS", new String[]{
                        "Algorithms", "Operating Systems", "Computer Networks", "Discrete Mathematics", "Programming Paradigms",
                        "Compilers", "Databases", "Distributed Computing", "Cryptography", "Formal Methods",
                        "Advanced Algorithms", "Quantum Computing", "Systems Security", "Capstone Project", "Research Seminar"},
                "MSc Applied Mathematics", "MSC-MATH", new String[]{
                        "Real Analysis", "Linear Algebra", "Probability Theory", "Numerical Methods", "Optimization",
                        "Stochastic Processes", "Partial Differential Equations", "Statistical Modeling", "Functional Analysis", "Graph Theory",
                        "Financial Mathematics", "Machine Learning Theory", "Dynamical Systems", "Capstone Project", "Research Seminar"});

        seedUniversity("HEC Paris", "HEC", "hec.fr", "Claire", "Dupont",
                new String[][]{{"Peter", "Drucker"}, {"Adam", "Smith"}, {"John", "Keynes"},
                        {"Esther", "Duflo"}, {"Michael", "Porter"}, {"Rosa", "Luxemburg"}},
                "MSc Management", "MSC-MGT", new String[]{
                        "Microeconomics", "Financial Accounting", "Marketing", "Organizational Behavior", "Business Statistics",
                        "Strategy", "Operations Management", "Corporate Finance", "Leadership", "Business Law",
                        "Entrepreneurship", "Digital Transformation", "Negotiation", "Capstone Project", "Consulting Lab"},
                "MSc Finance", "MSC-FIN", new String[]{
                        "Financial Markets", "Accounting", "Applied Microeconomics", "Quantitative Methods", "Corporate Finance",
                        "Derivatives", "Asset Management", "Risk Management", "Fixed Income", "Financial Modeling",
                        "Private Equity", "Behavioral Finance", "FinTech", "Capstone Project", "Research Project"});

        log.info("[DemoDataSeeder] Done. All demo accounts use password: {}", DEMO_PASSWORD);
    }

    private void seedUniversity(String uniName, String code, String domain,
                                String adminFirst, String adminLast,
                                String[][] lecturerNames,
                                String p1Name, String p1Code, String[] p1Courses,
                                String p2Name, String p2Code, String[] p2Courses) {
        if (universityRepository.existsByCode(code)) {
            log.info("[DemoDataSeeder] {} already exists — skipping.", code);
            return;
        }

        University uni = university(uniName, code, domain);
        adminLogin(adminFirst, adminLast, "admin@" + domain, uni);

        // Programmes first (lecturers and cohorts reference them).
        Programme p1 = programme(p1Name, p1Code, uni);
        Programme p2 = programme(p2Name, p2Code, uni);

        // 6 lecturers: first three teach programme 1, last three teach programme 2.
        List<Lecturer> lecturers = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Programme assigned = (i < 3) ? p1 : p2;
            String ref = "LEC-" + code + "-" + String.format("%03d", i + 1);
            lecturers.add(lecturer(lecturerNames[i][0], lecturerNames[i][1], ref, domain, uni, List.of(assigned)));
        }
        List<Lecturer> p1Lecturers = lecturers.subList(0, 3);
        List<Lecturer> p2Lecturers = lecturers.subList(3, 6);

        // Two intakes; both programmes run in each intake.
        Cohort spring = cohort(CohortSeason.SPRING, 2026, uni);
        Cohort fall = cohort(CohortSeason.FALL, 2025, uni);
        attach(p1, spring, fall);
        attach(p2, spring, fall);

        // 3 semesters x 5 courses per programme.
        buildCurriculum(p1, p1Courses, p1Lecturers);
        buildCurriculum(p2, p2Courses, p2Lecturers);

        // Students enrolled per programme + intake.
        int ref = 1;
        ref = seedStudents(p1, spring, code, domain, uni, ref);
        ref = seedStudents(p1, fall, code, domain, uni, ref);
        ref = seedStudents(p2, spring, code, domain, uni, ref);
        ref = seedStudents(p2, fall, code, domain, uni, ref);

        log.info("[DemoDataSeeder] Seeded {} (2 programmes, 2 intakes, 6 lecturers, "
                + "6 semesters, 30 courses, {} students).", code, ref - 1);
    }

    /** Builds 3 semesters, each with 5 courses, round-robin assigning the programme's lecturers. */
    private void buildCurriculum(Programme programme, String[] courseNames, List<Lecturer> lecturers) {
        for (int s = 0; s < 3; s++) {
            Semester semester = semester("Semester " + (s + 1), s + 1, programme);
            for (int c = 0; c < 5; c++) {
                int idx = s * 5 + c;
                Lecturer lecturer = lecturers.get(idx % lecturers.size());
                String courseCode = programme.getCode() + "-" + (s + 1) + String.format("%02d", c + 1);
                course(courseNames[idx], courseCode, programme, semester, lecturer);
            }
        }
    }

    private int seedStudents(Programme programme, Cohort cohort, String code, String domain,
                             University uni, int startRef) {
        for (int i = 0; i < STUDENTS_PER_PROGRAMME_PER_INTAKE; i++) {
            String[] name = STUDENT_NAMES[studentSeq++ % STUDENT_NAMES.length];
            String ref = "STU-" + code + "-" + String.format("%03d", startRef++);
            student(name[0], name[1], ref, domain, uni, programme, cohort);
        }
        return startRef;
    }

    // -------------------------------------------------------------- helpers
    private University university(String name, String code, String domain) {
        return universityRepository.save(
                University.builder().name(name).code(code).domain(domain).build());
    }

    private Programme programme(String name, String code, University uni) {
        return programmeRepository.save(Programme.builder()
                .name(name).code(code).description(name + " at " + uni.getName())
                .university(uni).status(ProgrammeStatus.ACTIVE).build());
    }

    private Cohort cohort(CohortSeason season, int year, University uni) {
        String label = (season == CohortSeason.SPRING ? "Spring " : "Fall ") + year;
        return cohortRepository.save(Cohort.builder()
                .name(label).season(season).academicYear(year)
                .university(uni).status(CohortStatus.ONGOING).build());
    }

    /** Attaches a programme to one or more intakes (join table owned by Programme.cohorts). */
    private void attach(Programme programme, Cohort... cohorts) {
        programme.setCohorts(new ArrayList<>(List.of(cohorts)));
        programmeRepository.save(programme);
    }

    private Semester semester(String name, int order, Programme programme) {
        return semesterRepository.save(Semester.builder()
                .name(name).orderIndex(order).programme(programme).build());
    }

    private Course course(String name, String code, Programme programme, Semester semester, Lecturer lecturer) {
        return courseRepository.save(Course.builder()
                .name(name).code(code).description(name)
                .programme(programme).semester(semester).lecturer(lecturer)
                .status(CourseStatus.ACTIVE).build());
    }

    private void adminLogin(String first, String last, String email, University uni) {
        login(first, last, email, Role.ROLE_UNI_ADMIN, uni.getId());
    }

    private Lecturer lecturer(String first, String last, String ref, String domain,
                              University uni, List<Programme> programmes) {
        String email = professionalEmail(first, last, domain);
        Lecturer lecturer = lecturerRepository.save(Lecturer.builder()
                .firstName(first).lastName(last)
                .email(email)
                .lecturerRef(ref)
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .programmes(new ArrayList<>(programmes))
                .status(LecturerStatus.ACTIVE)
                .build());
        login(first, last, email, Role.ROLE_LECTURER, uni.getId());
        return lecturer;
    }

    private void student(String first, String last, String ref, String domain,
                         University uni, Programme programme, Cohort cohort) {
        String email = professionalEmail(first, last, domain);
        studentRepository.save(Student.builder()
                .firstName(first).lastName(last)
                .email(email)
                .studentRef(ref)
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .programme(programme)
                .cohort(cohort)
                .status(StudentStatus.ACTIVE)
                .build());
        login(first, last, email, Role.ROLE_STUDENT, uni.getId());
    }

    private void login(String first, String last, String email, Role role, Long universityId) {
        appUserRepository.save(AppUser.builder()
                .firstName(first).surname(last)
                .email(email)
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .role(role)
                .dateOfBirth(demoDob(role, email))
                .universityId(universityId)
                .build());
    }

    private LocalDate demoDob(Role role, String emailSeed) {
        int seed = Math.abs(emailSeed.hashCode());
        int age = switch (role) {
            case ROLE_STUDENT  -> 21 + seed % 5;
            case ROLE_LECTURER -> 35 + seed % 21;
            default            -> 38 + seed % 15;
        };
        int month = 1 + seed % 12;
        int day = 1 + seed % 28;
        return LocalDate.now().minusYears(age).withMonth(month).withDayOfMonth(day);
    }

    private String professionalEmail(String first, String last, String domain) {
        return sanitize(last) + "-" + sanitize(first) + "@" + domain;
    }

    private String sanitize(String s) {
        return s.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
