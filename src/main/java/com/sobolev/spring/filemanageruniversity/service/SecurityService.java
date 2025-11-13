package com.sobolev.spring.filemanageruniversity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class SecurityService {

    @Value("${filemanager.base.directory:./files}")
    private String baseDirectory;

    @Value("${filemanager.max.file.size:104857600}")
    private long maxFileSize; // 100MB по умолчанию

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
            throw new SecurityException("Размер файла превышает максимально допустимый: " + maxFileSize + " байт");
        }
    }

    public Path getBaseDirectory() {
        return Paths.get(baseDirectory).toAbsolutePath().normalize();
    }

    public void ensureBaseDirectoryExists() {
        File baseDir = getBaseDirectory().toFile();
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }
}

