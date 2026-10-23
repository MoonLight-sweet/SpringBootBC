package com.codeclinic.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    @Test
    void passwordChangeInvalidatesExistingToken() {
        JwtService service = new JwtService("0123456789abcdef0123456789abcdef", 24);
        String token = service.createToken(7L, "student", "first-bcrypt-hash");
        JwtService.TokenIdentity identity = service.parse(token);
        assertEquals(7L, identity.userId());
        assertTrue(service.matchesPasswordVersion(identity, "first-bcrypt-hash"));
        assertFalse(service.matchesPasswordVersion(identity, "new-bcrypt-hash"));
    }

    @Test
    void rejectsShortConfiguredKey() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("predictable", 24));
    }
}
