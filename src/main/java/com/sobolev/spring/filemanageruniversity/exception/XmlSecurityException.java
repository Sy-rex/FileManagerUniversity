package com.sobolev.spring.filemanageruniversity.exception;

/**
 * Исключение, возникающее при обнаружении потенциальной XML атаки
 */
public class XmlSecurityException extends SecurityException {
    
    public XmlSecurityException(String message) {
        super("Обнаружена потенциальная XML/JSON атака: " + message);
    }
    
    public XmlSecurityException(String message, Throwable cause) {
        super("Обнаружена потенциальная XML атака: " + message, cause);
    }
}

