package com.hisabnikash.app.domain

import com.hisabnikash.app.domain.model.FormInputSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the form-input rule behind every edit form: the stored record is used
 * to seed the fields exactly once, and afterwards in-progress typing always
 * wins — a database emission must never blank or overwrite user input.
 */
class FormInputSyncTest {

    @Test
    fun `new form seeds from stored record once`() {
        val (loaded, value) = FormInputSync.mergeLoaded(
            loaded = false,
            current = "typed while loading",
            stored = "Nusrat Jahan"
        )
        assertTrue(loaded)
        assertEquals("Nusrat Jahan", value)
    }

    @Test
    fun `typing after load is never overwritten`() {
        var (loaded, value) = FormInputSync.mergeLoaded(
            loaded = false,
            current = "",
            stored = "Nusrat Jahan"
        )
        assertEquals("Nusrat Jahan", value)
        value = "Nusra"
        val (stillLoaded, afterEdit) = FormInputSync.mergeLoaded(
            loaded = loaded,
            current = value,
            stored = "Nusrat Jahan"
        )
        assertTrue(stillLoaded)
        assertEquals("Nusra", afterEdit)
    }

    @Test
    fun `typing before the record loads is never replaced`() {
        val (loaded, value) = FormInputSync.mergeLoaded(
            loaded = false,
            current = "Nus",
            stored = "Nusrat Jahan",
            currentIsBlank = false
        )
        assertTrue(loaded)
        assertEquals("Nus", value)
    }

    @Test
    fun `no stored record keeps current values and stays unloaded`() {
        val (loaded, value) = FormInputSync.mergeLoaded(
            loaded = false,
            current = "S",
            stored = null
        )
        assertFalse(loaded)
        assertEquals("S", value)
    }

    @Test
    fun `loaded form ignores later database changes`() {
        val (loaded, value) = FormInputSync.mergeLoaded(
            loaded = true,
            current = "Sh",
            stored = "Store updated elsewhere"
        )
        assertTrue(loaded)
        assertEquals("Sh", value)
    }
}
