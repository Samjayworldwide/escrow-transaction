package com.samjay.authentication_service.controllers;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwtPublicKeyController {

    private final RSAKey rsaKey;

    @GetMapping("/oauth2/jwks")
    public Map<String, Object> getJwtPublicKey() {

        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}
