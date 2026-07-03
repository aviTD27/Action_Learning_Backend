package fr.epita.dto.Response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AnnouncementResponse {

    /** ID of the recipient row (used for mark-as-read) */
    private Long recipientId;

    private Long announcementId;
    private String subject;
    private String message;
    private String senderName;
    private String senderRole;
    private String audience;
    private Instant sentAt;
    private boolean read;
}
