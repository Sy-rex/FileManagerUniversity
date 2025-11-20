package com.sobolev.spring.filemanageruniversity.exception;

/**
 * Исключение, возникающее при ошибке валидации данных
 */
public class ValidationException extends FileManagerException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

