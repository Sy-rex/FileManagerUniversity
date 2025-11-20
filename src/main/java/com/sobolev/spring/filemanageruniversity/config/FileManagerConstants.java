package com.sobolev.spring.filemanageruniversity.config;

/**
 * Константы для файлового менеджера
 */
public final class
FileManagerConstants {
    
    private FileManagerConstants() {
        // Утилитный класс
    }
    
    // Размеры буферов для операций ввода-вывода
    public static final int BUFFER_SIZE = 8192; // 8 KB
    
    // Лимиты для ZIP архивов
    public static final int ZIP_MAX_ENTRIES = 10000; // Максимальное количество записей в архиве
    
    // Константы для форматирования размеров файлов
    public static final long BYTES_PER_KB = 1024L;
    public static final long BYTES_PER_MB = BYTES_PER_KB * 1024L;
    public static final long BYTES_PER_GB = BYTES_PER_MB * 1024L;
    
    // Константы для валидации
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 1;
    
    // Алгоритм хеширования для checksum
    public static final String CHECKSUM_ALGORITHM = "SHA-256";
    
    // Кодировка по умолчанию
    public static final String DEFAULT_CHARSET = "UTF-8";
}

