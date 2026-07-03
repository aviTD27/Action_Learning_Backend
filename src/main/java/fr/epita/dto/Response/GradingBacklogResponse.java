package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingBacklogResponse {
    /** Turned-in submissions that have no released grade yet. */
    private long awaitingGrades;
    /** Total turned-in (submission, student) pairs. */
    private long turnedIn;
    /** Released grades. */
    private long released;
}
