package com.rupeek.hotel.api;

import com.rupeek.hotel.domain.exception.BookingHoldExpiredException;
import com.rupeek.hotel.domain.exception.CapacityExceededException;
import com.rupeek.hotel.domain.exception.DomainException;
import com.rupeek.hotel.domain.exception.DomainValidationException;
import com.rupeek.hotel.domain.exception.EntityNotFoundException;
import com.rupeek.hotel.domain.exception.IdempotencyKeyConflictException;
import com.rupeek.hotel.domain.exception.InsufficientInventoryException;
import com.rupeek.hotel.domain.exception.InvalidStateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 9457 problem. The base class already handles the Spring MVC errors
 * (bad JSON, missing header, failed validation) as 400. This class adds the domain errors.
 * Every problem has an extra {@code error} property with the exception name.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({DomainValidationException.class, CapacityExceededException.class})
    ProblemDetail badRequest(DomainException e) {
        return problem(400, e);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail notFound(EntityNotFoundException e) {
        return problem(404, e);
    }

    @ExceptionHandler({InvalidStateTransitionException.class, InsufficientInventoryException.class,
            BookingHoldExpiredException.class})
    ProblemDetail conflict(DomainException e) {
        return problem(409, e);
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    ProblemDetail unprocessable(IdempotencyKeyConflictException e) {
        return problem(422, e);
    }

    /** The message and the stack trace stay in the log. The client gets a fixed text. */
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        log.error("Unexpected error", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error. Try again later.");
        problem.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        problem.setProperty("error", "InternalServerError"); // not the class name: it says too much
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            problem.setProperty("error", ex.getClass().getSimpleName());
        }
        return response;
    }

    private static ProblemDetail problem(int status, DomainException e) {
        HttpStatus httpStatus = HttpStatus.valueOf(status);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(httpStatus, e.getMessage());
        problem.setTitle(httpStatus.getReasonPhrase());
        problem.setProperty("error", e.getClass().getSimpleName());
        return problem;
    }
}
