package fr.epita.controller;

import fr.epita.dto.Response.CohortBenchmarkResponse;
import fr.epita.dto.Response.GradeDistributionResponse;
import fr.epita.dto.Response.TenantSummaryResponse;
import fr.epita.dto.Response.TrendPointResponse;
import fr.epita.model.AppUser;
import fr.epita.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/tenant/summary")
    public ResponseEntity<TenantSummaryResponse> tenantSummary(
            @RequestParam(required = false) Long universityId,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(analyticsService.tenantSummary(resolve(universityId, currentUser)));
    }

    @GetMapping("/tenant/trends")
    public ResponseEntity<List<TrendPointResponse>> tenantTrends(
            @RequestParam(required = false) Long universityId,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(analyticsService.tenantTrends(resolve(universityId, currentUser)));
    }

    @GetMapping("/tenant/grade-distribution")
    public ResponseEntity<List<GradeDistributionResponse>> gradeDistribution(
            @RequestParam(required = false) Long universityId,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(analyticsService.gradeDistribution(resolve(universityId, currentUser)));
    }

    @GetMapping("/tenant/cohort-benchmark")
    public ResponseEntity<List<CohortBenchmarkResponse>> cohortBenchmark(
            @RequestParam(required = false) Long universityId,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(analyticsService.cohortBenchmark(resolve(universityId, currentUser)));
    }

    private Long resolve(Long universityId, AppUser currentUser) {
        if (universityId != null) return universityId;
        return currentUser != null ? currentUser.getUniversityId() : null;
    }
}
