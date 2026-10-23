package com.example.bodegalider.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTest {

    @Test
    fun iniciales() {
        assertEquals("AP", User("1", "Ana Pérez", UserRole.USER).initials)
        assertEquals("C", User("1", "carlos", UserRole.USER).initials)
    }

    @Test
    fun soloAdminsSonSupervisores() {
        assertTrue(User("1", "Roberto", UserRole.ADMIN).isSupervisor)
        assertFalse(User("2", "Carlos", UserRole.USER).isSupervisor)
    }
}
