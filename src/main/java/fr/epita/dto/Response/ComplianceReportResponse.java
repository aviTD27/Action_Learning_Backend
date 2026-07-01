package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReportResponse {

    private Long uploadId;
    private String fileName;
    private long fileSizeBytes;
    private boolean overallPass;

    private CheckResult fileType;
    private CheckResult naming;
    private CheckResult wordCount;
    private CheckResult headings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckResult {
        private String label;
        private boolean passed;
        private boolean skipped;
        private String message;
        private String detail;
    }
}
