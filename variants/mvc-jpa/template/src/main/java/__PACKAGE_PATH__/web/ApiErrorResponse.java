package __PACKAGE_NAME__.web;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiErrorResponse(
    OffsetDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> validationErrors
) {
}
