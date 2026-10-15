package com.codeclinic.user.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerTest {

    @Test
    void allowsMailSetupOnlyFromLoopbackAddress() {
        assertTrue(UserController.isLoopbackAddress("127.0.0.1"));
        assertTrue(UserController.isLoopbackAddress("::1"));
        assertFalse(UserController.isLoopbackAddress("192.168.1.10"));
        assertFalse(UserController.isLoopbackAddress("not-an-address"));
    }
}
