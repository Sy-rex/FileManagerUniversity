package com.sobolev.spring.filemanageruniversity.exception;

/**
 * Исключение, возникающее при обнаружении ZIP-бомбы
 */
public class ZipBombException extends SecurityException {
    
    public ZipBombException(String message) {
        super("Обнаружена потенциальная ZIP-бомба: " + message);
    }
    
    public ZipBombException(String message, Throwable cause) {
        super("Обнаружена потенциальная ZIP-бомба: " + message, cause);
    }
}

