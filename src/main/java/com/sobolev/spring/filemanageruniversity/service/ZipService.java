package com.sobolev.spring.filemanageruniversity.service;

import com.sobolev.spring.filemanageruniversity.config.FileManagerConstants;
import com.sobolev.spring.filemanageruniversity.exception.FileNotFoundException;
import com.sobolev.spring.filemanageruniversity.exception.SecurityException;
import com.sobolev.spring.filemanageruniversity.exception.ZipBombException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;

@Service
public class ZipService {

    private final SecurityService securityService;

    @Value("${filemanager.zip.max.ratio:100}")
    private int maxCompressionRatio; // Максимальная степень сжатия (1:100)

    @Value("${filemanager.zip.max.uncompressed.size:1073741824}")
    private long maxUncompressedSize; // 1GB максимальный размер распакованных данных

    @Autowired
    public ZipService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void createZipArchive(String zipPath, String... filePaths) throws IOException {
        Path validatedZipPath = securityService.validateAndNormalizePath(zipPath);
        
        // Создаем директорию, если не существует
        Files.createDirectories(validatedZipPath.getParent());
        
        long totalUncompressedSize = 0;
        
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(
                new BufferedOutputStream(Files.newOutputStream(validatedZipPath)))) {
            
            for (String filePath : filePaths) {
                Path validatedFilePath = securityService.validateAndNormalizePath(filePath);
                File file = validatedFilePath.toFile();
                
                if (!file.exists()) {
                    throw new FileNotFoundException(filePath);
                }
                
                if (file.isDirectory()) {
                    addDirectoryToZip(zos, file, "", totalUncompressedSize);
                } else {
                    totalUncompressedSize += addFileToZip(zos, file, "", totalUncompressedSize);
                }
            }
        }
    }

    private void addDirectoryToZip(ZipArchiveOutputStream zos, File directory, String basePath, long currentTotal) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addDirectoryToZip(zos, file, basePath + file.getName() + "/", currentTotal);
                } else {
                    currentTotal += addFileToZip(zos, file, basePath, currentTotal);
                }
            }
        }
    }

    private long addFileToZip(ZipArchiveOutputStream zos, File file, String basePath, long currentTotal) throws IOException {
        long fileSize = file.length();
        long newTotal = currentTotal + fileSize;
        
        // Защита от ZIP-бомб: проверяем общий размер распакованных данных
        if (newTotal > maxUncompressedSize) {
            throw new SecurityException("Превышен максимальный размер распакованных данных. Возможна ZIP-бомба!");
        }
        
        securityService.validateFileSize(fileSize);
        
        ZipArchiveEntry entry = new ZipArchiveEntry(basePath + file.getName());
        entry.setSize(fileSize);
        zos.putArchiveEntry(entry);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[FileManagerConstants.BUFFER_SIZE];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }
        
        zos.closeArchiveEntry();
        return fileSize;
    }

    public void extractZipArchive(String zipPath, String extractToPath) throws IOException {
        Path validatedZipPath = securityService.validateAndNormalizePath(zipPath);
        Path validatedExtractPath = securityService.validateAndNormalizePath(extractToPath);
        
        File zipFile = validatedZipPath.toFile();
        if (!zipFile.exists()) {
            throw new FileNotFoundException(zipPath);
        }
        
        securityService.validateFileSize(zipFile.length());
        
        long zipFileSize = zipFile.length();
        
        // ПЕРВЫЙ ПРОХОД: Валидация архива БЕЗ извлечения файлов
        validateZipArchive(validatedZipPath, zipFileSize);
        
        // ВТОРОЙ ПРОХОД: Извлечение файлов только после успешной валидации
        // Создаем директорию для извлечения только после валидации
        Files.createDirectories(validatedExtractPath);
        
        // Список извлеченных файлов для очистки при ошибке
        java.util.List<Path> extractedFiles = new java.util.ArrayList<>();
        
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(
                new BufferedInputStream(Files.newInputStream(validatedZipPath)))) {
            
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                Path entryPath = validatedExtractPath.resolve(entry.getName()).normalize();
                
                // Защита от Path Traversal при извлечении
                if (!entryPath.startsWith(validatedExtractPath)) {
                    cleanupExtractedFiles(extractedFiles);
                    throw new SecurityException("Попытка извлечения файла вне целевой директории (Path Traversal)");
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    
                    // Безопасное копирование с ограничением размера
                    long entrySize = entry.getSize();
                    try (OutputStream os = Files.newOutputStream(entryPath)) {
                        byte[] buffer = new byte[FileManagerConstants.BUFFER_SIZE];
                        long written = 0;
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            written += len;
                            // Дополнительная проверка во время записи
                            if (written > entrySize && entrySize > 0) {
                                cleanupExtractedFiles(extractedFiles);
                                try {
                                    Files.deleteIfExists(entryPath);
                                } catch (IOException ignored) {}
                                throw new ZipBombException("Размер извлекаемого файла превышает заявленный: " + written + " > " + entrySize);
                            }
                            // Проверка общего размера во время записи
                            if (written > maxUncompressedSize) {
                                cleanupExtractedFiles(extractedFiles);
                                try {
                                    Files.deleteIfExists(entryPath);
                                } catch (IOException ignored) {}
                                throw new ZipBombException("Превышен максимальный размер распакованных данных во время извлечения");
                            }
                            os.write(buffer, 0, len);
                        }
                        // Файл успешно извлечен - добавляем в список
                        extractedFiles.add(entryPath);
                    } catch (Exception e) {
                        // При любой ошибке очищаем уже извлеченные файлы
                        cleanupExtractedFiles(extractedFiles);
                        throw e;
                    }
                }
            }
        } catch (ZipBombException | SecurityException e) {
            // При обнаружении ZIP-бомбы очищаем все извлеченные файлы
            cleanupExtractedFiles(extractedFiles);
            throw e;
        } catch (Exception e) {
            // При любой другой ошибке также очищаем
            cleanupExtractedFiles(extractedFiles);
            throw new IOException("Ошибка при извлечении архива: " + e.getMessage(), e);
        }
    }
    
    /**
     * Удаляет все частично извлеченные файлы при обнаружении ошибки
     */
    private void cleanupExtractedFiles(java.util.List<Path> extractedFiles) {
        for (Path file : extractedFiles) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // Игнорируем ошибки при удалении
            }
        }
        extractedFiles.clear();
    }
    }
    
    /**
     * Валидирует ZIP архив перед извлечением
     * Проверяет все записи БЕЗ записи файлов на диск
     */
    private void validateZipArchive(Path zipPath, long zipFileSize) throws IOException {
        long totalUncompressedSize = 0;
        int entryCount = 0;
        
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(
                new BufferedInputStream(Files.newInputStream(zipPath)))) {
            
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                entryCount++;
                
                // Защита от ZIP-бомб: проверяем количество записей ДО извлечения
                if (entryCount > FileManagerConstants.ZIP_MAX_ENTRIES) {
                    throw new ZipBombException("Слишком много записей в архиве: " + entryCount);
                }
                
                long entrySize = entry.getSize();
                if (entrySize > 0) {
                    // Защита от ZIP-бомб: проверяем степень сжатия ДО извлечения
                    if (entrySize > zipFileSize * maxCompressionRatio) {
                        throw new ZipBombException("Подозрительно высокая степень сжатия: " + entrySize + " байт");
                    }
                    
                    totalUncompressedSize += entrySize;
                    
                    // Защита от ZIP-бомб: проверяем общий размер распакованных данных ДО извлечения
                    if (totalUncompressedSize > maxUncompressedSize) {
                        throw new ZipBombException("Превышен максимальный размер распакованных данных: " + totalUncompressedSize + " байт");
                    }
                }
                
                // Пропускаем данные записи (не записываем на диск, только читаем для валидации)
                if (!entry.isDirectory()) {
                    byte[] buffer = new byte[FileManagerConstants.BUFFER_SIZE];
                    long read = 0;
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        read += len;
                        // Проверка реального размера во время чтения
                        if (read > entrySize && entrySize > 0) {
                            throw new ZipBombException("Размер записи превышает заявленный: " + read + " > " + entrySize);
                        }
                        // Проверка общего размера во время чтения
                        if (read > maxUncompressedSize) {
                            throw new ZipBombException("Превышен максимальный размер распакованных данных при валидации");
                        }
                    }
                }
            }
        }
    }
}

