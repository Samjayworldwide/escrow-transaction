package com.samjay.driver_service.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public class ApplicationException extends RuntimeException {

    private final String message;

    private final HttpStatus httpStatus;
}
