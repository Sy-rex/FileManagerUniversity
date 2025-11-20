package com.sobolev.spring.filemanageruniversity.exception;

/**
 * Исключение, возникающее при нарушении безопасности
 */
public class SecurityException extends FileManagerException {
    
    public SecurityException(String message) {
        super(message);
    }
    
    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}

