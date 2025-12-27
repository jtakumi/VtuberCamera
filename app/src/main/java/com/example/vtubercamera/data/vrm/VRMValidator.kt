package com.example.vtubercamera.data.vrm

import android.content.Context
import android.net.Uri
import java.io.IOException

/**
 * Utility class for validating VRM files
 */
object VRMValidator {
    
    private const val MAX_FILE_SIZE = 100 * 1024 * 1024L // 100MB
    private const val MIN_FILE_SIZE = 1024L // 1KB
    private val SUPPORTED_VERSIONS = listOf("1.0", "0.0")
    
    /**
     * Validate VRM file format and structure
     */
    fun validateVRMFile(context: Context, uri: Uri): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                errors.add(ValidationError.fileNotFound(uri.toString()))
                return ValidationResult.Invalid(errors)
            }
            
            inputStream.use { stream ->
                // Check file size
                val fileSize = stream.available().toLong()
                
                if (fileSize < MIN_FILE_SIZE) {
                    errors.add(ValidationError.critical(
                        ValidationError.ErrorType.INVALID_FORMAT,
                        "File is too small to be a valid VRM file"
                    ))
                }
                
                if (fileSize > MAX_FILE_SIZE) {
                    errors.add(ValidationError.fileSizeExceeded(fileSize, MAX_FILE_SIZE))
                }
                
                // Read and validate file header
                val headerValidation = validateFileHeader(stream)
                if (headerValidation.isInvalid()) {
                    errors.addAll((headerValidation as ValidationResult.Invalid).errors)
                }
                
                // Additional format validation
                val formatValidation = validateVRMFormat(stream)
                if (formatValidation.isInvalid()) {
                    errors.addAll((formatValidation as ValidationResult.Invalid).errors)
                }
            }
            
        } catch (e: SecurityException) {
            errors.add(ValidationError.permissionDenied(e.message))
        } catch (e: IOException) {
            errors.add(ValidationError.critical(
                ValidationError.ErrorType.CORRUPTED_DATA,
                "Cannot read file: ${e.message}"
            ))
        } catch (e: Exception) {
            errors.add(ValidationError.unknownError(e.message))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Validate file header to ensure it's a glTF/VRM file
     */
    private fun validateFileHeader(stream: java.io.InputStream): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        try {
            // Reset stream to beginning
            stream.mark(20)
            
            // Read first 12 bytes for glTF header
            val header = ByteArray(12)
            val bytesRead = stream.read(header)
            
            if (bytesRead < 12) {
                errors.add(ValidationError.invalidFormat("File header is incomplete"))
                return ValidationResult.Invalid(errors)
            }
            
            // Check glTF magic number
            val magic = String(header, 0, 4, Charsets.UTF_8)
            if (magic != "glTF") {
                errors.add(ValidationError.invalidFormat("Not a valid glTF file (missing magic number)"))
            }
            
            // Check glTF version (bytes 4-7)
            val version = java.nio.ByteBuffer.wrap(header, 4, 4)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .int
            
            if (version != 2) {
                errors.add(ValidationError.warning(
                    ValidationError.ErrorType.UNSUPPORTED_VERSION,
                    "glTF version $version may not be fully supported"
                ))
            }
            
            // Reset stream position
            stream.reset()
            
        } catch (e: Exception) {
            errors.add(ValidationError.critical(
                ValidationError.ErrorType.CORRUPTED_DATA,
                "Error reading file header: ${e.message}"
            ))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Validate VRM-specific format requirements
     */
    private fun validateVRMFormat(stream: java.io.InputStream): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        try {
            // This is a simplified validation
            // In a real implementation, you would:
            // 1. Parse the JSON chunk to find VRM extensions
            // 2. Validate VRM version compatibility
            // 3. Check required VRM fields
            // 4. Validate mesh and material references
            
            // For now, we'll do basic checks
            val buffer = ByteArray(1024)
            val bytesRead = stream.read(buffer)
            
            if (bytesRead > 0) {
                val content = String(buffer, 0, bytesRead, Charsets.UTF_8)
                
                // Look for VRM extension marker
                if (!content.contains("VRM") && !content.contains("vrm")) {
                    errors.add(ValidationError.warning(
                        ValidationError.ErrorType.MISSING_REQUIRED_DATA,
                        "VRM extension not found in file"
                    ))
                }
            }
            
        } catch (e: Exception) {
            errors.add(ValidationError.warning(
                ValidationError.ErrorType.UNKNOWN_ERROR,
                "Could not validate VRM format: ${e.message}"
            ))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Quick validation for file extension and basic properties
     */
    fun quickValidate(uri: Uri): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Check file extension
        val path = uri.path ?: uri.toString()
        if (!path.lowercase().endsWith(".vrm")) {
            errors.add(ValidationError.warning(
                ValidationError.ErrorType.INVALID_FORMAT,
                "File does not have .vrm extension"
            ))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Validate VRM metadata for completeness
     */
    fun validateMetadata(metadata: VRMMetadata): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        if (metadata.title.isBlank()) {
            errors.add(ValidationError.warning(
                ValidationError.ErrorType.INVALID_METADATA,
                "VRM title is missing"
            ))
        }
        
        if (metadata.author.isBlank()) {
            errors.add(ValidationError.warning(
                ValidationError.ErrorType.INVALID_METADATA,
                "VRM author is missing"
            ))
        }
        
        if (metadata.version.isBlank()) {
            errors.add(ValidationError.warning(
                ValidationError.ErrorType.INVALID_METADATA,
                "VRM version is missing"
            ))
        } else if (!SUPPORTED_VERSIONS.contains(metadata.version)) {
            errors.add(ValidationError.warning(
                ValidationError.ErrorType.UNSUPPORTED_VERSION,
                "VRM version ${metadata.version} may not be fully supported"
            ))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}