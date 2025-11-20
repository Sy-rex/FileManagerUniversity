package com.sobolev.spring.filemanageruniversity.service;

import com.sobolev.spring.filemanageruniversity.exception.SecurityException;
import com.sobolev.spring.filemanageruniversity.exception.ValidationException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class SecurityService {

    @Value("${filemanager.base.directory:./files}")
    private String baseDirectory;

    @Value("${filemanager.max.file.size:104857600}")
    private long maxFileSize; // 100MB по умолчанию
    
    @PostConstruct
    public void validateConfiguration() {
        if (baseDirectory == null || baseDirectory.trim().isEmpty()) {
            throw new ValidationException("Базовая директория не может быть пустой");
        }
        if (maxFileSize <= 0) {
            throw new ValidationException("Максимальный размер файла должен быть положительным числом");
        }
    }

    public Path validateAndNormalizePath(String userPath) {
        if (userPath == null || userPath.isEmpty()) {
            throw new SecurityException("Путь не может быть пустым");
        }

        // Нормализуем базовую директорию
        Path basePath = Paths.get(baseDirectory).toAbsolutePath().normalize();
        
        // Создаем полный путь
        Path resolvedPath = basePath.resolve(userPath).toAbsolutePath().normalize();
        
        // Проверяем, что результирующий путь находится внутри базовой директории
        if (!resolvedPath.startsWith(basePath)) {
            throw new SecurityException("Попытка доступа к файлу вне разрешенной директории (Path Traversal)");
        }
        
        return resolvedPath;
    }

    public void validateFileSize(long fileSize) {
        if (fileSize < 0) {
            throw new SecurityException("Размер файла не может быть отрицательным");
        }
        if (fileSize > maxFileSize) {
            String maxSizeFormatted = formatFileSize(maxFileSize);
            String actualSizeFormatted = formatFileSize(fileSize);
            throw new SecurityException(String.format(
                "Размер файла (%s) превышает максимально допустимый: %s (%d байт)",
                actualSizeFormatted, maxSizeFormatted, maxFileSize
            ));
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public Path getBaseDirectory() {
        return Paths.get(baseDirectory).toAbsolutePath().normalize();
    }

    public void ensureBaseDirectoryExists() {
        Path basePath = getBaseDirectory();
        try {
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            if (!Files.isDirectory(basePath)) {
                throw new ValidationException("Базовый путь не является директорией: " + basePath);
            }
            if (!Files.isWritable(basePath)) {
                throw new SecurityException("Нет прав на запись в базовую директорию: " + basePath);
            }
        } catch (java.io.IOException e) {
            throw new ValidationException("Не удалось создать базовую директорию: " + basePath, e);
        }
    }
}

