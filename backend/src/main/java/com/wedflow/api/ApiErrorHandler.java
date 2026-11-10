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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class ApiErrorHandler {
 private static final Logger LOG=LoggerFactory.getLogger(ApiErrorHandler.class);
 private ResponseEntity<ProblemDetail> problem(HttpStatus status,String detail,HttpServletRequest request){
  ProblemDetail body=ProblemDetail.forStatusAndDetail(status,detail);
  body.setProperty("requestId",request.getAttribute("requestId"));
  String code=switch(status.value()){
   case 400 -> "INVALID_REQUEST";
   case 401 -> "UNAUTHORIZED";
   case 403 -> "FORBIDDEN";
   case 404 -> "NOT_FOUND";
   case 405 -> "METHOD_NOT_ALLOWED";
   case 409 -> "CONFLICT";
   case 413 -> "PAYLOAD_TOO_LARGE";
   case 415 -> "UNSUPPORTED_MEDIA_TYPE";
   case 422 -> "VALIDATION_FAILED";
   case 429 -> "RATE_LIMITED";
   case 502 -> "PROVIDER_FAILURE";
   case 503 -> "SERVICE_UNAVAILABLE";
   default -> status.is5xxServerError()?"SERVER_ERROR":"REQUEST_ERROR";
  };
  body.setProperty("code",code);
  return ResponseEntity.status(status).header("Cache-Control","no-store").body(body);
 }
 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<ProblemDetail> status(ResponseStatusException ex,HttpServletRequest request){
  HttpStatus status=HttpStatus.resolve(ex.getStatusCode().value());
  if(status==null)status=HttpStatus.INTERNAL_SERVER_ERROR;
  String detail=status.is5xxServerError()?"The service could not complete the request. Please use the support reference.":ex.getReason();
  if(detail==null||detail.isBlank()||detail.length()>180||
     detail.matches("(?is).*(exception|stacktrace|password|secret|token|jdbc|sql|<|>).*"))
     detail=status.is5xxServerError()?"The service could not complete the request. Please use the support reference.":status.getReasonPhrase();
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
 @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,MissingServletRequestParameterException.class})
 public ResponseEntity<ProblemDetail> malformed(Exception ex,HttpServletRequest request){
  return problem(HttpStatus.BAD_REQUEST,"The request format or parameter value is invalid.",request);
 }
 @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
 public ResponseEntity<ProblemDetail> method(HttpRequestMethodNotSupportedException ex,HttpServletRequest request){
  return problem(HttpStatus.METHOD_NOT_ALLOWED,"This action is not supported for the requested endpoint.",request);
 }
 @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
 public ResponseEntity<ProblemDetail> media(HttpMediaTypeNotSupportedException ex,HttpServletRequest request){
  return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Unsupported content type.",request);
 }
 @ExceptionHandler(NoResourceFoundException.class)
 public ResponseEntity<ProblemDetail> notFound(NoResourceFoundException ex,HttpServletRequest request){
  return problem(HttpStatus.NOT_FOUND,"The requested endpoint was not found.",request);
 }
 @ExceptionHandler(AccessDeniedException.class)
 public ResponseEntity<ProblemDetail> forbidden(AccessDeniedException ex,HttpServletRequest request){
  return problem(HttpStatus.FORBIDDEN,"You do not have permission to perform this action.",request);
 }
 @ExceptionHandler(Exception.class)
 public ResponseEntity<ProblemDetail> unexpected(Exception ex,HttpServletRequest request){
  LOG.error("Unhandled API exception requestId={} type={}",request.getAttribute("requestId"),ex.getClass().getSimpleName());
  return problem(HttpStatus.INTERNAL_SERVER_ERROR,"An unexpected error occurred. Please contact support with the request ID.",request);
 }
}
