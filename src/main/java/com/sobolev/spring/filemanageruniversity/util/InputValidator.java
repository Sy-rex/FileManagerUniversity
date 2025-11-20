package com.sobolev.spring.filemanageruniversity.util;

import com.sobolev.spring.filemanageruniversity.exception.ValidationException;

/**
 * Утилитный класс для валидации входных данных
 */
public final class InputValidator {
    
    private InputValidator() {
        // Утилитный класс
    }
    
    /**
     * Валидирует путь к файлу/директории
     * @param path путь для валидации
     * @param fieldName название поля (для сообщения об ошибке)
     * @return валидированный путь
     * @throws ValidationException если путь пустой или null
     */
    public static String validatePath(String path, String fieldName) {
        if (path == null || path.trim().isEmpty()) {
            throw new ValidationException(fieldName + " не может быть пустым");
        }
        return path.trim();
    }
    
    /**
     * Валидирует путь к файлу/директории с подсказкой
     * @param path путь для валидации
     * @param fieldName название поля
     * @param example пример правильного формата
     * @return валидированный путь
     * @throws ValidationException если путь пустой или null
     */
    public static String validatePath(String path, String fieldName, String example) {
        if (path == null || path.trim().isEmpty()) {
            throw new ValidationException(
                String.format("%s не может быть пустым. Пример: %s", fieldName, example)
            );
        }
        return path.trim();
    }
    
    /**
     * Валидирует непустую строку
     * @param value значение для валидации
     * @param fieldName название поля
     * @return валидированное значение
     * @throws ValidationException если значение пустое или null
     */
    public static String validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " не может быть пустым");
        }
        return value.trim();
    }
    
    /**
     * Проверяет, что строка не пустая, возвращает Optional
     * @param value значение для проверки
     * @return true если не пустое, false иначе
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

