package fr.epita.service;

import fr.epita.dto.Response.CohortBenchmarkResponse;
import fr.epita.dto.Response.GradeDistributionResponse;
import fr.epita.dto.Response.TenantSummaryResponse;
import fr.epita.dto.Response.TrendPointResponse;
import fr.epita.enums.CohortStatus;
import fr.epita.enums.GradeStatus;
import fr.epita.enums.LecturerStatus;
import fr.epita.enums.StudentStatus;
import fr.epita.model.Cohort;
import fr.epita.model.Student;
import fr.epita.model.StudentGrade;
import fr.epita.model.Submission;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.ProgrammeRepository;
import fr.epita.repository.StudentGradeRepository;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private static final int TREND_MONTHS = 6;

    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final ProgrammeRepository programmeRepository;
    private final CohortRepository cohortRepository;
    private final SubmissionRepository submissionRepository;
    private final StudentGradeRepository studentGradeRepository;

    //  Tenant summary 
    @Transactional(readOnly = true)
    public TenantSummaryResponse tenantSummary(Long universityId) {
        requireUniversity(universityId);

        List<Student> students = studentRepository.findByProgramme_UniversityId(universityId);
        long totalLecturers = lecturerRepository.findDistinctByProgrammes_UniversityId(universityId).size();
        long activeLecturers = lecturerRepository.findDistinctByProgrammes_UniversityId(universityId).stream()
                .filter(l -> l.getStatus() == LecturerStatus.ACTIVE).count();
        long totalProgrammes = programmeRepository.findByUniversityId(universityId).size();
        List<Cohort> cohorts = cohortRepository.findByProgramme_UniversityId(universityId);
        long totalSubmissions = submissionRepository.findByCohort_Programme_University_Id(universityId).size();

        List<StudentGrade> released = releasedGrades(universityId);
        YearMonth thisMonth = YearMonth.now(ZONE);

        long gradedThisMonth = released.stream()
                .filter(g -> g.getGradedAt() != null && YearMonth.from(toLocalMonth(g.getGradedAt())).equals(thisMonth))
                .count();

        double avgScorePct = released.stream().mapToDouble(this::scorePct).average().orElse(0.0);

        return TenantSummaryResponse.builder()
                .totalStudents(students.size())
                .activeStudents(students.stream().filter(s -> s.getStatus() == StudentStatus.ACTIVE).count())
                .totalLecturers(totalLecturers)
                .activeLecturers(activeLecturers)
                .totalProgrammes(totalProgrammes)
                .totalCohorts(cohorts.size())
                .activeCohorts(cohorts.stream().filter(c -> c.getStatus() == CohortStatus.ONGOING).count())
                .totalSubmissions(totalSubmissions)
                .releasedGrades(released.size())
                .gradedThisMonth(gradedThisMonth)
                .avgScorePct(round1(avgScorePct))
                .build();
    }

    //  6-month trends 
    @Transactional(readOnly = true)
    public List<TrendPointResponse> tenantTrends(Long universityId) {
        requireUniversity(universityId);

        List<Submission> submissions = submissionRepository.findByCohort_Programme_University_Id(universityId);
        List<StudentGrade> released = releasedGrades(universityId);

        List<TrendPointResponse> series = new ArrayList<>();
        YearMonth current = YearMonth.now(ZONE);

        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);

            long subs = submissions.stream()
                    .filter(s -> s.getCreatedAt() != null && YearMonth.from(toLocalMonth(s.getCreatedAt())).equals(ym))
                    .count();

            double avg = released.stream()
                    .filter(g -> g.getGradedAt() != null && YearMonth.from(toLocalMonth(g.getGradedAt())).equals(ym))
                    .mapToDouble(this::scorePct)
                    .average().orElse(0.0);

            series.add(TrendPointResponse.builder()
                    .month(ym.atDay(1).format(MONTH_LABEL))
                    .submissions(subs)
                    .avgScore(round1(avg))
                    .build());
        }
        return series;
    }

    //  Grade-band distribution
    @Transactional(readOnly = true)
    public List<GradeDistributionResponse> gradeDistribution(Long universityId) {
        requireUniversity(universityId);

        List<StudentGrade> released = releasedGrades(universityId);
        long distinction = 0, good = 0, pass = 0, fail = 0;
        for (StudentGrade g : released) {
            double pct = scorePct(g);
            if (pct >= 85) distinction++;
            else if (pct >= 70) good++;
            else if (pct >= 50) pass++;
            else fail++;
        }
        return List.of(
                GradeDistributionResponse.builder().band("Distinction").count(distinction).build(),
                GradeDistributionResponse.builder().band("Good").count(good).build(),
                GradeDistributionResponse.builder().band("Pass").count(pass).build(),
                GradeDistributionResponse.builder().band("Fail").count(fail).build()
        );
    }

    //  Within-university cohort benchmark
    @Transactional(readOnly = true)
    public List<CohortBenchmarkResponse> cohortBenchmark(Long universityId) {
        requireUniversity(universityId);

        List<Cohort> cohorts = cohortRepository.findByProgramme_UniversityId(universityId);
        List<Student> students = studentRepository.findByProgramme_UniversityId(universityId);
        List<Submission> submissions = submissionRepository.findByCohort_Programme_University_Id(universityId);
        List<StudentGrade> released = releasedGrades(universityId);

        List<CohortBenchmarkResponse> rows = new ArrayList<>();
        for (Cohort c : cohorts) {
            Long cid = c.getId();

            long cohortStudents = students.stream()
                    .filter(s -> s.getCohort() != null && cid.equals(s.getCohort().getId()))
                    .count();
            long cohortSubs = submissions.stream()
                    .filter(s -> s.getCohort() != null && cid.equals(s.getCohort().getId()))
                    .count();
            List<StudentGrade> cohortGrades = released.stream()
                    .filter(g -> g.getSubmission() != null && g.getSubmission().getCohort() != null
                            && cid.equals(g.getSubmission().getCohort().getId()))
                    .toList();
            double avg = cohortGrades.stream().mapToDouble(this::scorePct).average().orElse(0.0);

            rows.add(CohortBenchmarkResponse.builder()
                    .cohortId(cid)
                    .cohortName(c.getName())
                    .programmeName(c.getProgramme() != null ? c.getProgramme().getName() : null)
                    .students(cohortStudents)
                    .submissions(cohortSubs)
                    .releasedGrades(cohortGrades.size())
                    .avgScorePct(round1(avg))
                    .build());
        }

        rows.sort(Comparator.comparingDouble(CohortBenchmarkResponse::getAvgScorePct).reversed());
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRank(i + 1);
        }
        return rows;
    }

    //  Helpers
    private List<StudentGrade> releasedGrades(Long universityId) {
        return studentGradeRepository.findBySubmission_Cohort_Programme_University_Id(universityId).stream()
                .filter(g -> g.getStatus() == GradeStatus.RELEASED)
                .toList();
    }

    private double scorePct(StudentGrade g) {
        Submission s = g.getSubmission();
        int max = (s != null) ? s.getMaxPoints() : 0;
        if (max <= 0) return 0.0;
        double pct = g.getGrade() / max * 100.0;
        if (pct < 0) return 0.0;
        if (pct > 100) return 100.0;
        return pct;
    }

    private YearMonth toLocalMonth(Instant instant) {
        return YearMonth.from(instant.atZone(ZONE).toLocalDate());
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private void requireUniversity(Long universityId) {
        if (universityId == null) {
            throw new IllegalArgumentException("A university context is required for analytics.");
        }
    }
}
