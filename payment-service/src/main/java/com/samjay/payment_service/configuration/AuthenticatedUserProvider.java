package com.samjay.payment_service.configuration;

import com.samjay.payment_service.dtos.responses.UserIdentifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import static com.samjay.payment_service.utility.AppExtensions.USERNAME_CLAIM_KEY;
import static com.samjay.payment_service.utility.AppExtensions.USER_ID_CLAIM_KEY;

@Component
public class AuthenticatedUserProvider {

    public UserIdentifier getCurrentLoggedInUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {

            String userId = jwtAuth.getToken().getClaim(USER_ID_CLAIM_KEY);

            String username = jwtAuth.getToken().getClaimAsString(USERNAME_CLAIM_KEY);

            String email = jwtAuth.getToken().getSubject();

            return new UserIdentifier(userId, username, email);
        }

        throw new IllegalStateException("No authenticated JWT user found");
    }
}
