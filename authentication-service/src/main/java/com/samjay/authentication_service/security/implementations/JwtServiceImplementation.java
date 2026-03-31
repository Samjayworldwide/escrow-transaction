package com.samjay.authentication_service.security.implementations;

import com.samjay.authentication_service.entities.User;
import com.samjay.authentication_service.enumerations.Roles;
import com.samjay.authentication_service.globalexceptionhandlers.exceptions.ApplicationException;
import com.samjay.authentication_service.security.interfaces.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtServiceImplementation implements JwtService {

    private final JwtEncoder jwtEncoder;

    @Value("${jwt.issuer}")
    private String jwtIssuer;

    @Value("${jwt.expiry}")
    private long jwtExpiry;

    @Override
    public String generateToken(Authentication authentication) {

        Instant now = Instant.now();

        User user = (User) authentication.getPrincipal();

        if (user == null) {

            log.error("User details not found in authentication object");

            throw new ApplicationException("User details not found in authentication object", HttpStatus.FORBIDDEN);
        }

        String username = user.getRole() == Roles.CUSTOMER ? user.getUsername() : "";

        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        JwtClaimsSet claims = JwtClaimsSet
                .builder()
                .issuer(jwtIssuer)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwtExpiry))
                .id(UUID.randomUUID().toString())
                .claim("authorities", authorities)
                .claim("userId", user.getId().toString())
                .claim("username", username)
                .build();

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
