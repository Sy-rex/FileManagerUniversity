package com.sobolev.spring.filemanageruniversity.service;

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
                    throw new IOException("Файл не найден: " + filePath);
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
            byte[] buffer = new byte[8192];
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
            throw new IOException("ZIP архив не найден: " + zipPath);
        }
        
        securityService.validateFileSize(zipFile.length());
        
        // Создаем директорию для извлечения
        Files.createDirectories(validatedExtractPath);
        
        long totalUncompressedSize = 0;
        int entryCount = 0;
        long zipFileSize = zipFile.length();
        
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(
                new BufferedInputStream(Files.newInputStream(validatedZipPath)))) {
            
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                entryCount++;
                
                // Защита от ZIP-бомб: проверяем количество записей
                if (entryCount > 10000) {
                    throw new SecurityException("Слишком много записей в архиве. Возможна ZIP-бомба!");
                }
                
                long entrySize = entry.getSize();
                if (entrySize > 0) {
                    // Защита от ZIP-бомб: проверяем степень сжатия
                    if (entrySize > zipFileSize * maxCompressionRatio) {
                        throw new SecurityException("Подозрительно высокая степень сжатия. Возможна ZIP-бомба!");
                    }
                    
                    totalUncompressedSize += entrySize;
                    
                    // Защита от ZIP-бомб: проверяем общий размер распакованных данных
                    if (totalUncompressedSize > maxUncompressedSize) {
                        throw new SecurityException("Превышен максимальный размер распакованных данных. Возможна ZIP-бомба!");
                    }
                }
                
                Path entryPath = validatedExtractPath.resolve(entry.getName()).normalize();
                
                // Защита от Path Traversal при извлечении
                if (!entryPath.startsWith(validatedExtractPath)) {
                    throw new SecurityException("Попытка извлечения файла вне целевой директории (Path Traversal)");
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    
                    // Безопасное копирование с ограничением размера
                    try (OutputStream os = Files.newOutputStream(entryPath)) {
                        byte[] buffer = new byte[8192];
                        long written = 0;
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            written += len;
                            if (written > entrySize && entrySize > 0) {
                                throw new SecurityException("Размер извлекаемого файла превышает заявленный. Возможна ZIP-бомба!");
                            }
                            os.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }
}

