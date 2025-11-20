package com.sobolev.spring.filemanageruniversity.service;

import com.sobolev.spring.filemanageruniversity.entity.FileEntity;
import com.sobolev.spring.filemanageruniversity.entity.OperationType;
import com.sobolev.spring.filemanageruniversity.entity.User;
import com.sobolev.spring.filemanageruniversity.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sobolev.spring.filemanageruniversity.config.FileManagerConstants;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class FileService {

    private final FileRepository fileRepository;
    private final SecurityService securityService;
    private final AuditService auditService;

    @Autowired
    public FileService(FileRepository fileRepository, SecurityService securityService, AuditService auditService) {
        this.fileRepository = fileRepository;
        this.securityService = securityService;
        this.auditService = auditService;
    }


    @Transactional
    public String readFile(String filePath, User user) throws IOException {
        Path validatedPath = securityService.validateAndNormalizePath(filePath);
        File file = validatedPath.toFile();
        
        if (!file.exists()) {
            throw new FileNotFoundException("Файл не найден: " + filePath);
        }
        
        securityService.validateFileSize(file.length());
        
        // Защита от Race Conditions: используем FileLock
        try (FileInputStream fis = new FileInputStream(file);
             FileChannel channel = fis.getChannel();
             FileLock lock = channel.lock(0, Long.MAX_VALUE, true)) { // Shared lock для чтения
            
            byte[] content = Files.readAllBytes(validatedPath);
            String contentStr = new String(content, FileManagerConstants.DEFAULT_CHARSET);
            
            // Логируем операцию
            FileEntity fileEntity = findOrCreateFileEntity(filePath, file, user);
            auditService.logOperation(user, OperationType.READ, fileEntity, "Чтение файла: " + filePath);
            
            return contentStr;
        }
    }

    @Transactional
    public void writeFile(String filePath, String content, User user) throws IOException {
        Path validatedPath = securityService.validateAndNormalizePath(filePath);
        
        byte[] contentBytes = content.getBytes(FileManagerConstants.DEFAULT_CHARSET);
        securityService.validateFileSize(contentBytes.length);
        
        // Создаем директорию, если не существует
        Files.createDirectories(validatedPath.getParent());
        
        File file = validatedPath.toFile();
        boolean fileExists = file.exists();
        
        // Защита от Race Conditions: используем FileLock
        try (FileOutputStream fos = new FileOutputStream(file);
             FileChannel channel = fos.getChannel();
             FileLock lock = channel.lock(0, Long.MAX_VALUE, false)) { // Exclusive lock для записи
            
            // Безопасная запись файла через FileChannel
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(contentBytes);
            channel.write(buffer);
            channel.force(true); // Принудительная запись на диск
            
            // Обновляем или создаем запись в БД
            FileEntity fileEntity = findOrCreateFileEntity(filePath, file, user);
            fileEntity.setSize((long) contentBytes.length);
            fileEntity.setCreatedAt(LocalDateTime.now());
            fileEntity.setChecksum(calculateChecksum(file));
            fileRepository.save(fileEntity);
            
            // Логируем операцию
            OperationType operationType = fileExists ? OperationType.MODIFY : OperationType.CREATE;
            auditService.logOperation(user, operationType, fileEntity, 
                (fileExists ? "Изменение" : "Создание") + " файла: " + filePath);
        }
    }

    @Transactional
    public void deleteFile(String filePath, User user) throws IOException {
        Path validatedPath = securityService.validateAndNormalizePath(filePath);
        File file = validatedPath.toFile();
        
        if (!file.exists()) {
            throw new FileNotFoundException("Файл не найден: " + filePath);
        }
        
        // Находим файл в БД перед блокировкой
        Optional<FileEntity> fileEntityOpt = fileRepository.findByLocation(validatedPath.toString());
        
        // Защита от Race Conditions: используем FileLock перед удалением
        // RandomAccessFile в режиме "rw" позволяет получить эксклюзивную блокировку
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel channel = raf.getChannel();
             FileLock lock = channel.lock(0, Long.MAX_VALUE, false)) {
            
            // Файл заблокирован, можно безопасно удалять
        }
        
        // Сохраняем информацию о файле перед удалением
        Long fileId = null;
        String fileName = file.getName();
        if (fileEntityOpt.isPresent()) {
            FileEntity fileEntity = fileEntityOpt.get();
            fileId = fileEntity.getId();
            fileName = fileEntity.getFilename();
        }
        
        // Удаляем файл после освобождения блокировки
        Files.delete(validatedPath);

        // Удаляем из БД и логируем операцию
        if (fileEntityOpt.isPresent()) {
            FileEntity fileEntity = fileEntityOpt.get();
            // Удаляем файл из БД
            fileRepository.delete(fileEntity);
            // Flush для гарантии удаления перед логированием
            fileRepository.flush();
            // Логируем операцию без ссылки на файл (так как он уже удален)
            auditService.logOperation(user, OperationType.DELETE, null, 
                "Удаление файла: " + fileName + " (ID: " + fileId + ", путь: " + filePath + ")");
        } else {
            // Файл удален, но записи в БД нет - логируем без привязки к файлу
            auditService.logOperation(user, OperationType.DELETE, null, "Удаление файла (не найден в БД): " + filePath);
        }
    }

    @Transactional
    public void copyFile(String sourcePath, String destPath, User user) throws IOException {
        Path validatedSourcePath = securityService.validateAndNormalizePath(sourcePath);
        Path validatedDestPath = securityService.validateAndNormalizePath(destPath);
        
        File sourceFile = validatedSourcePath.toFile();
        if (!sourceFile.exists()) {
            throw new FileNotFoundException("Исходный файл не найден: " + sourcePath);
        }
        
        securityService.validateFileSize(sourceFile.length());
        
        // Создаем директорию назначения, если не существует
        Files.createDirectories(validatedDestPath.getParent());
        
        // Безопасное копирование с использованием NIO
        Files.copy(validatedSourcePath, validatedDestPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        
        // Создаем запись в БД для нового файла
        FileEntity destFileEntity = findOrCreateFileEntity(destPath, validatedDestPath.toFile(), user);
        fileRepository.save(destFileEntity);
        
        // Логируем операцию
        auditService.logOperation(user, OperationType.CREATE, destFileEntity, 
            "Копирование файла из " + sourcePath + " в " + destPath);
    }

    @Transactional
    public void moveFile(String sourcePath, String destPath, User user) throws IOException {
        Path validatedSourcePath = securityService.validateAndNormalizePath(sourcePath);
        Path validatedDestPath = securityService.validateAndNormalizePath(destPath);
        
        File sourceFile = validatedSourcePath.toFile();
        if (!sourceFile.exists()) {
            throw new FileNotFoundException("Исходный файл не найден: " + sourcePath);
        }
        
        // Создаем директорию назначения, если не существует
        Files.createDirectories(validatedDestPath.getParent());
        
        // Безопасное перемещение с использованием NIO (атомарная операция)
        Files.move(validatedSourcePath, validatedDestPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        
        // Обновляем запись в БД
        Optional<FileEntity> fileEntityOpt = fileRepository.findByLocation(validatedSourcePath.toString());
        if (fileEntityOpt.isPresent()) {
            FileEntity fileEntity = fileEntityOpt.get();
            fileEntity.setLocation(validatedDestPath.toString());
            fileRepository.save(fileEntity);
            
            // Логируем операцию
            auditService.logOperation(user, OperationType.MODIFY, fileEntity, 
                "Перемещение файла из " + sourcePath + " в " + destPath);
        }
    }

    public List<String> listFiles(String directoryPath, User user) throws IOException {
        Path validatedPath = securityService.validateAndNormalizePath(directoryPath);
        File directory = validatedPath.toFile();
        
        if (!directory.exists()) {
            throw new FileNotFoundException("Директория не найдена: " + directoryPath);
        }
        
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Указанный путь не является директорией: " + directoryPath);
        }

        auditService.logOperation(user, OperationType.READ, null, "Просмотр директории: " + directoryPath);
        
        return Files.list(validatedPath)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
    }

    public FileEntity getFileInfo(String filePath, User user) {
        Path validatedPath = securityService.validateAndNormalizePath(filePath);
        return fileRepository.findByLocation(validatedPath.toString())
                .orElse(null);
    }

    public List<FileEntity> getUserFiles(User user) {
        return fileRepository.findByOwner(user);
    }

    private FileEntity findOrCreateFileEntity(String filePath, File file, User user) {
        Path validatedPath = securityService.validateAndNormalizePath(filePath);
        Optional<FileEntity> existing = fileRepository.findByLocation(validatedPath.toString());
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        FileEntity fileEntity = new FileEntity(file.getName(), validatedPath.toString(), user);
        fileEntity.setSize(file.length());
        fileEntity.setCreatedAt(LocalDateTime.now());
        fileEntity.setFileType(getFileExtension(file.getName()));
        fileEntity.setChecksum(calculateChecksum(file));
        return fileRepository.save(fileEntity);
    }

    private String calculateChecksum(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance(FileManagerConstants.CHECKSUM_ALGORITHM);
            try (FileInputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                byte[] buffer = new byte[FileManagerConstants.BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
}

