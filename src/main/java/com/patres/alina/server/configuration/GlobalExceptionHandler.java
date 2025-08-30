package com.patres.alina.server.configuration;//package com.patres.alina.server.configuration;
//
//import com.patres.alina.common.exception.ErrorResponse;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.context.request.WebRequest;
//import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
//
//@ControllerAdvice
//public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
//        ErrorResponse errorResponse = new ErrorResponse(
//                HttpStatus.INTERNAL_SERVER_ERROR.value(),
//                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
//                ex.getMessage(),
//                request.getDescription(false)
//        );
//
//        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//
//}