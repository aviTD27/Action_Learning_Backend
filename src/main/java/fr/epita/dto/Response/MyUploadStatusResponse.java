package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyUploadStatusResponse {
    private Long uploadId;
    private boolean turnedIn;
    private String fileName;
    private String turnedInAt;
    private boolean compliancePassed;
    private boolean late;
    private boolean reopened;
}
