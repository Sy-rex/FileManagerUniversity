package com.sobolev.spring.filemanageruniversity.exception;

/**
 * Исключение, возникающее когда файл не найден
 */
public class FileNotFoundException extends FileManagerException {
    
    private final String filePath;
    
    public FileNotFoundException(String filePath) {
        super("Файл не найден: " + filePath);
        this.filePath = filePath;
    }
    
    public FileNotFoundException(String filePath, Throwable cause) {
        super("Файл не найден: " + filePath, cause);
        this.filePath = filePath;
    }
    
    public String getFilePath() {
        return filePath;
    }
}

