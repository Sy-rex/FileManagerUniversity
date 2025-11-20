package com.sobolev.spring.filemanageruniversity.exception;

/**
 * Базовое исключение для файлового менеджера
 */
public class FileManagerException extends RuntimeException {
    
    public FileManagerException(String message) {
        super(message);
    }
    
    public FileManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}

