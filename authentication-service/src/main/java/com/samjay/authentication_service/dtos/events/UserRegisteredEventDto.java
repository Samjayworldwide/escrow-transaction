package com.samjay.authentication_service.dtos.events;

import com.samjay.authentication_service.enumerations.Roles;

import java.util.UUID;

public record UserRegisteredEventDto(UUID userId, String firstname, String lastname, String email, String username,
                                     Roles role) {
}
