package com.example.vtubercamera.data

import com.example.vtubercamera.data.vrm.ValidationError
import com.example.vtubercamera.data.vrm.ValidationResult
import com.example.vtubercamera.data.vrm.VRMValidator
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for VRM validation and data models
 */
class VRMRepositoryTest {
    
    @Test
    fun `ValidationResult Valid should return true for isValid`() {
        val result = ValidationResult.Valid
        assertTrue(result.isValid())
        assertFalse(result.isInvalid())
    }
    
    @Test
    fun `ValidationResult Invalid should return false for isValid`() {
        val errors = listOf(
            ValidationError.fileNotFound("/test/path")
        )
        val result = ValidationResult.Invalid(errors)
        
        assertFalse(result.isValid())
        assertTrue(result.isInvalid())
        assertTrue(result.hasErrors())
        assertEquals(1, result.errors.size)
    }
    
    @Test
    fun `ValidationError critical should create critical error`() {
        val error = ValidationError.critical(
            ValidationError.ErrorType.FILE_NOT_FOUND,
            "Test error"
        )
        
        assertTrue(error.isCritical())
        assertFalse(error.isWarning())
        assertEquals(ValidationError.ErrorType.FILE_NOT_FOUND, error.type)
        assertEquals("Test error", error.message)
    }
    
    @Test
    fun `ValidationError warning should create warning error`() {
        val error = ValidationError.warning(
            ValidationError.ErrorType.INVALID_METADATA,
            "Test warning"
        )
        
        assertFalse(error.isCritical())
        assertTrue(error.isWarning())
        assertEquals(ValidationError.ErrorType.INVALID_METADATA, error.type)
        assertEquals("Test warning", error.message)
    }
    
    @Test
    fun `ValidationResult Invalid should identify critical errors`() {
        val errors = listOf(
            ValidationError.critical(ValidationError.ErrorType.FILE_NOT_FOUND, "Critical"),
            ValidationError.warning(ValidationError.ErrorType.INVALID_METADATA, "Warning")
        )
        val result = ValidationResult.Invalid(errors)
        
        assertTrue(result.hasCriticalErrors())
        assertEquals(1, result.getCriticalErrors().size)
        assertEquals(1, result.getWarnings().size)
    }
    
    @Test
    fun `ValidationResult Invalid should get first error message`() {
        val errors = listOf(
            ValidationError.critical(ValidationError.ErrorType.FILE_NOT_FOUND, "First error"),
            ValidationError.warning(ValidationError.ErrorType.INVALID_METADATA, "Second error")
        )
        val result = ValidationResult.Invalid(errors)
        
        assertEquals("First error", result.getFirstErrorMessage())
        assertEquals(2, result.getAllErrorMessages().size)
    }
}