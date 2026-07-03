package fr.epita.service;

import fr.epita.dto.Response.AtRiskStudentResponse;
import fr.epita.dto.Response.CohortBenchmarkResponse;
import fr.epita.dto.Response.GradeDistributionResponse;
import fr.epita.dto.Response.GradingBacklogResponse;
import fr.epita.dto.Response.LecturerOverviewResponse;
import fr.epita.dto.Response.LecturerWorkloadResponse;
import fr.epita.dto.Response.TenantSummaryResponse;
import fr.epita.dto.Response.TrendPointResponse;
import fr.epita.enums.CohortStatus;
import fr.epita.enums.GradeStatus;
import fr.epita.enums.LecturerStatus;
import fr.epita.enums.StudentStatus;
import fr.epita.enums.SubmissionStatus;
import fr.epita.model.Cohort;
import fr.epita.model.Lecturer;
import fr.epita.model.Student;
import fr.epita.model.StudentGrade;
import fr.epita.model.Submission;
import fr.epita.model.SubmissionUpload;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.ProgrammeRepository;
import fr.epita.repository.StudentGradeRepository;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.SubmissionRepository;
import fr.epita.repository.SubmissionUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final SubmissionUploadRepository uploadRepository;

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

    //  Grading backlog — turned-in submissions with no released grade yet
    @Transactional(readOnly = true)
    public GradingBacklogResponse gradingBacklog(Long universityId) {
        requireUniversity(universityId);
        List<Submission> submissions = submissionRepository.findByCohort_Programme_University_Id(universityId);
        Set<String> releasedKeys = releasedKeys(universityId);
        Set<String> turnedInKeys = turnedInKeys(submissions);
        long awaiting = turnedInKeys.stream().filter(k -> !releasedKeys.contains(k)).count();
        return GradingBacklogResponse.builder()
                .awaitingGrades(awaiting)
                .turnedIn(turnedInKeys.size())
                .released(releasedKeys.size())
                .build();
    }

    //  At-risk students — low average and/or repeated missed submissions
    @Transactional(readOnly = true)
    public List<AtRiskStudentResponse> atRiskStudents(Long universityId) {
        requireUniversity(universityId);
        List<Student> students = studentRepository.findByProgramme_UniversityId(universityId);
        List<Submission> submissions = submissionRepository.findByCohort_Programme_University_Id(universityId);
        List<StudentGrade> released = releasedGrades(universityId);
        Set<String> turnedInKeys = turnedInKeys(submissions);
        LocalDateTime now = LocalDateTime.now();

        List<AtRiskStudentResponse> out = new ArrayList<>();
        for (Student st : students) {
            Long sid = st.getId();
            Long cohortId = st.getCohort() != null ? st.getCohort().getId() : null;

            List<StudentGrade> myGrades = released.stream()
                    .filter(g -> g.getStudent() != null && sid.equals(g.getStudent().getId()))
                    .toList();
            Double avg = myGrades.isEmpty() ? null
                    : round1(myGrades.stream().mapToDouble(this::scorePct).average().orElse(0.0));

            long missed = 0;
            if (cohortId != null) {
                for (Submission s : submissions) {
                    if (s.getCohort() == null || !cohortId.equals(s.getCohort().getId())) continue;
                    if (s.getStatus() != SubmissionStatus.PUBLISHED) continue;
                    if (!now.isAfter(s.deadline())) continue; // deadline not passed yet
                    if (!turnedInKeys.contains(s.getId() + ":" + sid)) missed++;
                }
            }

            boolean lowAvg = avg != null && avg < 50.0;
            boolean manyMissed = missed >= 2;
            if (!lowAvg && !manyMissed) continue;

            StringBuilder reason = new StringBuilder();
            if (lowAvg) reason.append("Low average (").append(avg).append("%)");
            if (manyMissed) {
                if (reason.length() > 0) reason.append(" · ");
                reason.append("Missed ").append(missed).append(missed == 1 ? " submission" : " submissions");
            }

            out.add(AtRiskStudentResponse.builder()
                    .studentId(sid)
                    .studentName(st.getFirstName() + " " + st.getLastName())
                    .studentRef(st.getStudentRef())
                    .cohortName(st.getCohort() != null ? st.getCohort().getName() : null)
                    .programmeName(st.getProgramme() != null ? st.getProgramme().getName() : null)
                    .avgScorePct(avg)
                    .gradedCount(myGrades.size())
                    .missedSubmissions(missed)
                    .reason(reason.toString())
                    .build());
        }
        out.sort(Comparator
                .comparingLong(AtRiskStudentResponse::getMissedSubmissions).reversed()
                .thenComparing(r -> r.getAvgScorePct() == null ? Double.MAX_VALUE : r.getAvgScorePct()));
        return out;
    }

    //  Lecturer workload — assignments, cohorts taught, grading backlog per lecturer
    @Transactional(readOnly = true)
    public List<LecturerWorkloadResponse> lecturerWorkload(Long universityId) {
        requireUniversity(universityId);
        List<Lecturer> lecturers = lecturerRepository.findDistinctByProgrammes_UniversityId(universityId);
        List<Submission> submissions = submissionRepository.findByCohort_Programme_University_Id(universityId);
        List<Cohort> cohorts = cohortRepository.findByProgramme_UniversityId(universityId);
        Set<String> releasedKeys = releasedKeys(universityId);

        List<LecturerWorkloadResponse> out = new ArrayList<>();
        for (Lecturer l : lecturers) {
            Long lid = l.getId();
            List<Submission> mySubs = submissions.stream()
                    .filter(s -> s.getLecturer() != null && lid.equals(s.getLecturer().getId()))
                    .toList();
            long cohortsTaught = cohorts.stream()
                    .filter(c -> c.getLecturers() != null
                            && c.getLecturers().stream().anyMatch(x -> lid.equals(x.getId())))
                    .count();
            Set<String> myTurnedIn = turnedInKeys(mySubs);
            long backlog = myTurnedIn.stream().filter(k -> !releasedKeys.contains(k)).count();

            out.add(LecturerWorkloadResponse.builder()
                    .lecturerId(lid)
                    .lecturerName(l.getFirstName() + " " + l.getLastName())
                    .assignments(mySubs.size())
                    .cohorts(cohortsTaught)
                    .gradingBacklog(backlog)
                    .build());
        }
        out.sort(Comparator.comparingLong(LecturerWorkloadResponse::getGradingBacklog).reversed());
        return out;
    }

    //  Lecturer dashboard overview (resolved by the authenticated lecturer's email)
    @Transactional(readOnly = true)
    public LecturerOverviewResponse lecturerOverview(String email, Long universityId) {
        Lecturer lecturer = lecturerRepository.findByEmail(email).orElse(null);
        if (lecturer == null) {
            return LecturerOverviewResponse.builder()
                    .needsGrading(List.of()).atRisk(List.of())
                    .gradeDistribution(emptyBands()).recentActivity(List.of())
                    .build();
        }
        Long lid = lecturer.getId();
        List<Submission> subs = submissionRepository.findByLecturerId(lid);

        List<StudentGrade> allUniGrades = (universityId != null)
                ? studentGradeRepository.findBySubmission_Cohort_Programme_University_Id(universityId)
                : new ArrayList<>();
        List<StudentGrade> myGrades = allUniGrades.stream()
                .filter(g -> g.getSubmission() != null && g.getSubmission().getLecturer() != null
                        && lid.equals(g.getSubmission().getLecturer().getId()))
                .toList();

        Set<String> gradedKeys = new HashSet<>();
        for (StudentGrade g : myGrades) {
            if (g.getStudent() != null && g.getSubmission() != null) {
                gradedKeys.add(g.getSubmission().getId() + ":" + g.getStudent().getId());
            }
        }

        long compliancePassed = 0, complianceFailed = 0, onTime = 0, late = 0, backlog = 0;
        List<LecturerOverviewResponse.NeedsGradingItem> needs = new ArrayList<>();
        List<LecturerOverviewResponse.ActivityItem> activity = new ArrayList<>();

        for (Submission s : subs) {
            long awaiting = 0;
            Instant oldest = null;
            Set<String> seen = new HashSet<>();
            for (SubmissionUpload u : uploadRepository.findBySubmissionIdAndTurnedInTrue(s.getId())) {
                if (u.getStudent() == null) continue;
                String key = s.getId() + ":" + u.getStudent().getId();

                if (u.isCompliancePassed()) compliancePassed++; else complianceFailed++;

                boolean isLate = u.getTurnedInAt() != null
                        && u.getTurnedInAt().atZone(ZONE).toLocalDateTime().isAfter(s.deadline());
                if (isLate) late++; else onTime++;

                if (seen.add(key) && !gradedKeys.contains(key)) {
                    awaiting++;
                    if (u.getTurnedInAt() != null && (oldest == null || u.getTurnedInAt().isBefore(oldest))) {
                        oldest = u.getTurnedInAt();
                    }
                }

                if (u.getTurnedInAt() != null) {
                    activity.add(LecturerOverviewResponse.ActivityItem.builder()
                            .type("SUBMISSION")
                            .text(u.getStudent().getFirstName() + " " + u.getStudent().getLastName()
                                    + " submitted \"" + s.getTitle() + "\"")
                            .at(u.getTurnedInAt().toString())
                            .build());
                }
            }
            backlog += awaiting;
            if (awaiting > 0) {
                needs.add(LecturerOverviewResponse.NeedsGradingItem.builder()
                        .submissionId(s.getId())
                        .title(s.getTitle())
                        .cohortName(s.getCohort() != null ? s.getCohort().getName() : null)
                        .awaiting(awaiting)
                        .oldestSubmittedAt(oldest != null ? oldest.toString() : null)
                        .build());
            }
        }
        needs.sort(Comparator.comparing(n -> n.getOldestSubmittedAt() == null ? "9999" : n.getOldestSubmittedAt()));

        for (StudentGrade g : myGrades) {
            if (g.getStatus() == GradeStatus.RELEASED && g.getGradedAt() != null
                    && g.getStudent() != null && g.getSubmission() != null) {
                activity.add(LecturerOverviewResponse.ActivityItem.builder()
                        .type("GRADE")
                        .text("Released grade for " + g.getStudent().getFirstName() + " "
                                + g.getStudent().getLastName() + " — \"" + g.getSubmission().getTitle() + "\"")
                        .at(g.getGradedAt().toString())
                        .build());
            }
        }
        activity.sort(Comparator.comparing(LecturerOverviewResponse.ActivityItem::getAt, Comparator.reverseOrder()));
        List<LecturerOverviewResponse.ActivityItem> recent = activity.stream().limit(8).collect(Collectors.toList());

        List<StudentGrade> released = myGrades.stream().filter(g -> g.getStatus() == GradeStatus.RELEASED).toList();
        List<GradeDistributionResponse> dist = bandGrades(released);

        Set<Long> cohortIds = subs.stream()
                .map(s -> s.getCohort() != null ? s.getCohort().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<AtRiskStudentResponse> atRisk = atRiskForCohorts(cohortIds, subs, released);

        return LecturerOverviewResponse.builder()
                .gradingBacklog(backlog)
                .compliancePassed(compliancePassed)
                .complianceFailed(complianceFailed)
                .onTime(onTime)
                .late(late)
                .needsGrading(needs)
                .atRisk(atRisk)
                .gradeDistribution(dist)
                .recentActivity(recent)
                .build();
    }

    private List<AtRiskStudentResponse> atRiskForCohorts(Set<Long> cohortIds, List<Submission> subs,
                                                         List<StudentGrade> released) {
        if (cohortIds.isEmpty()) return List.of();
        Set<String> turnedInKeys = turnedInKeys(subs);
        LocalDateTime now = LocalDateTime.now();

        Map<Long, Student> studentMap = new LinkedHashMap<>();
        for (Long cid : cohortIds) {
            for (Student st : studentRepository.findByCohortId(cid)) studentMap.putIfAbsent(st.getId(), st);
        }

        List<AtRiskStudentResponse> out = new ArrayList<>();
        for (Student st : studentMap.values()) {
            Long sid = st.getId();
            List<StudentGrade> myG = released.stream()
                    .filter(g -> g.getStudent() != null && sid.equals(g.getStudent().getId()))
                    .toList();
            Double avg = myG.isEmpty() ? null
                    : round1(myG.stream().mapToDouble(this::scorePct).average().orElse(0.0));

            long missed = 0;
            Long cohortId = st.getCohort() != null ? st.getCohort().getId() : null;
            if (cohortId != null) {
                for (Submission s : subs) {
                    if (s.getCohort() == null || !cohortId.equals(s.getCohort().getId())) continue;
                    if (s.getStatus() != SubmissionStatus.PUBLISHED) continue;
                    if (!now.isAfter(s.deadline())) continue;
                    if (!turnedInKeys.contains(s.getId() + ":" + sid)) missed++;
                }
            }
            boolean lowAvg = avg != null && avg < 50.0;
            boolean manyMissed = missed >= 2;
            if (!lowAvg && !manyMissed) continue;

            StringBuilder reason = new StringBuilder();
            if (lowAvg) reason.append("Low average (").append(avg).append("%)");
            if (manyMissed) {
                if (reason.length() > 0) reason.append(" · ");
                reason.append("Missed ").append(missed).append(missed == 1 ? " submission" : " submissions");
            }
            out.add(AtRiskStudentResponse.builder()
                    .studentId(sid)
                    .studentName(st.getFirstName() + " " + st.getLastName())
                    .studentRef(st.getStudentRef())
                    .cohortName(st.getCohort() != null ? st.getCohort().getName() : null)
                    .programmeName(st.getProgramme() != null ? st.getProgramme().getName() : null)
                    .avgScorePct(avg)
                    .gradedCount(myG.size())
                    .missedSubmissions(missed)
                    .reason(reason.toString())
                    .build());
        }
        out.sort(Comparator
                .comparingLong(AtRiskStudentResponse::getMissedSubmissions).reversed()
                .thenComparing(r -> r.getAvgScorePct() == null ? Double.MAX_VALUE : r.getAvgScorePct()));
        return out;
    }

    private List<GradeDistributionResponse> bandGrades(List<StudentGrade> released) {
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

    private List<GradeDistributionResponse> emptyBands() {
        return bandGrades(List.of());
    }

    //  Helpers
    /** Set of "submissionId:studentId" for every turned-in upload across the given submissions. */
    private Set<String> turnedInKeys(List<Submission> submissions) {
        Set<String> keys = new HashSet<>();
        for (Submission s : submissions) {
            for (SubmissionUpload u : uploadRepository.findBySubmissionIdAndTurnedInTrue(s.getId())) {
                if (u.getStudent() != null) keys.add(s.getId() + ":" + u.getStudent().getId());
            }
        }
        return keys;
    }

    /** Set of "submissionId:studentId" for every released grade in the university. */
    private Set<String> releasedKeys(Long universityId) {
        Set<String> keys = new HashSet<>();
        for (StudentGrade g : releasedGrades(universityId)) {
            if (g.getSubmission() != null && g.getStudent() != null) {
                keys.add(g.getSubmission().getId() + ":" + g.getStudent().getId());
            }
        }
        return keys;
    }

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
