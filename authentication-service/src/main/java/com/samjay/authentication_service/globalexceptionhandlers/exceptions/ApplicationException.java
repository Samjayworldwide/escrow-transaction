package com.samjay.authentication_service.globalexceptionhandlers.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public class ApplicationException extends RuntimeException {

    private final String message;

    private final HttpStatus httpStatus;
}
