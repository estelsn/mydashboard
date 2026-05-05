package com.aifomo.dashboard.controller;

import com.aifomo.dashboard.collector.threads.session.ThreadsLoginBrowserLaunchException;
import com.aifomo.dashboard.service.InfoItemNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InfoItemNotFoundException.class)
    ProblemDetail handleInfoItemNotFound(InfoItemNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("InfoItem not found");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationError(MethodArgumentNotValidException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid request");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(ThreadsLoginBrowserLaunchException.class)
    ProblemDetail handleThreadsLoginBrowserLaunchError(ThreadsLoginBrowserLaunchException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Threads login browser launch failed");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }
}
