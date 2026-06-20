package dio.budgeting.infrastructure.http.response;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String message) {
}