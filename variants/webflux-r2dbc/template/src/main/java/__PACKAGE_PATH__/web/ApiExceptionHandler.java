package __PACKAGE_NAME__.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return buildProblemDetail(
            status,
            exception.getReason() != null ? exception.getReason() : status.getReasonPhrase()
        );
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ProblemDetail handleValidationException(WebExchangeBindException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail problemDetail = buildProblemDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception exception, ServerWebExchange exchange) {
        log.error("Unhandled exception for path={}", exchange.getRequest().getURI().getPath(), exception);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private ProblemDetail buildProblemDetail(HttpStatusCode statusCode, String detail) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setType(URI.create("about:blank"));
        return problemDetail;
    }
}
