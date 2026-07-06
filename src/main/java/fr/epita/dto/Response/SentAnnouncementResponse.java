package fr.epita.dto.Response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SentAnnouncementResponse {
    private Long id;
    private String subject;
    private String message;
    private String audience;
    private String cohortName;
    private int recipientCount;
    private Instant sentAt;
}
