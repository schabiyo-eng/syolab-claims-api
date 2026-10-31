package dev.syolab.claims.web;

import dev.syolab.claims.service.BadRequestException;
import dev.syolab.claims.service.ConflictException;
import dev.syolab.claims.service.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorMessage> badRequest(BadRequestException exception) {
    return ResponseEntity.badRequest().body(new ErrorMessage(exception.getMessage()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorMessage> malformed(HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest().body(new ErrorMessage("Request body is malformed."));
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorMessage> notFound(NotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorMessage(exception.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorMessage> conflict(ConflictException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorMessage(exception.getMessage()));
  }
}
