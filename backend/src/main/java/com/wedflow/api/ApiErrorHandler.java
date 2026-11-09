package com.wedflow.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class ApiErrorHandler {
 private static final Logger LOG=LoggerFactory.getLogger(ApiErrorHandler.class);
 private ResponseEntity<ProblemDetail> problem(HttpStatus status,String detail,HttpServletRequest request){
  ProblemDetail body=ProblemDetail.forStatusAndDetail(status,detail);
  body.setProperty("requestId",request.getAttribute("requestId"));
  return ResponseEntity.status(status).body(body);
 }
 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<ProblemDetail> status(ResponseStatusException ex,HttpServletRequest request){
  HttpStatus status=HttpStatus.resolve(ex.getStatusCode().value());
  if(status==null)status=HttpStatus.INTERNAL_SERVER_ERROR;
  String detail=status.is5xxServerError()?"An unexpected error occurred. Please contact support with the request ID.":ex.getReason();
  if(detail==null||detail.isBlank())detail=status.getReasonPhrase();
  return problem(status,detail,request);
 }
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<ProblemDetail> invalid(MethodArgumentNotValidException ex,HttpServletRequest request){
  return problem(HttpStatus.BAD_REQUEST,"Please check the submitted fields.",request);
 }
 @ExceptionHandler(DataIntegrityViolationException.class)
 public ResponseEntity<ProblemDetail> integrity(DataIntegrityViolationException ex,HttpServletRequest request){
  LOG.warn("Database constraint rejected requestId={}",request.getAttribute("requestId"));
  return problem(HttpStatus.CONFLICT,"The operation conflicts with existing records.",request);
 }
 @ExceptionHandler(Exception.class)
 public ResponseEntity<ProblemDetail> unexpected(Exception ex,HttpServletRequest request){
  LOG.error("Unhandled API exception requestId={} type={}",request.getAttribute("requestId"),ex.getClass().getSimpleName());
  return problem(HttpStatus.INTERNAL_SERVER_ERROR,"An unexpected error occurred. Please contact support with the request ID.",request);
 }
}
