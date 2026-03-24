package com.samjay.authentication_service.security.interfaces;

import org.springframework.security.core.Authentication;

public interface JwtService {

    String generateToken(Authentication authentication);
}
