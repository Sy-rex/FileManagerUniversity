package com.sobolev.spring.filemanageruniversity.util;

import com.sobolev.spring.filemanageruniversity.config.FileManagerConstants;
import com.sobolev.spring.filemanageruniversity.entity.FileEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Утилитный класс для форматирования вывода информации
 */
public final class OutputFormatter {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    
    private OutputFormatter() {
        // Утилитный класс
    }
    
    /**
     * Форматирует размер файла в читаемый вид (B, KB, MB, GB)
     */
    public static String formatFileSize(long bytes) {
        if (bytes < FileManagerConstants.BYTES_PER_KB) {
            return bytes + " B";
        }
        if (bytes < FileManagerConstants.BYTES_PER_MB) {
            return String.format("%.2f KB", bytes / (double) FileManagerConstants.BYTES_PER_KB);
        }
        if (bytes < FileManagerConstants.BYTES_PER_GB) {
            return String.format("%.2f MB", bytes / (double) FileManagerConstants.BYTES_PER_MB);
        }
        return String.format("%.2f GB", bytes / (double) FileManagerConstants.BYTES_PER_GB);
    }
    
    /**
     * Форматирует дату и время в читаемый вид
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "не указана";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }
    
    /**
     * Форматирует информацию о файле для вывода
     */
    public static String formatFileInfo(FileEntity file) {
        if (file == null) {
            return "Файл не найден";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("┌─────────────────────────────────────────────────┐\n");
        sb.append("│ Информация о файле                             │\n");
        sb.append("├─────────────────────────────────────────────────┤\n");
        sb.append(String.format("│ Имя:        %-35s │\n", truncate(file.getFilename(), 35)));
        sb.append(String.format("│ Размер:     %-35s │\n", formatFileSize(file.getSize() != null ? file.getSize() : 0)));
        sb.append(String.format("│ Дата:       %-35s │\n", formatDateTime(file.getCreatedAt())));
        sb.append(String.format("│ Тип:        %-35s │\n", file.getFileType() != null ? file.getFileType() : "неизвестен"));
        sb.append(String.format("│ Архивирован: %-34s │\n", file.getIsArchived() != null && file.getIsArchived() ? "да" : "нет"));
        if (file.getChecksum() != null) {
            sb.append(String.format("│ Checksum:   %-35s │\n", truncate(file.getChecksum(), 35)));
        }
        sb.append("└─────────────────────────────────────────────────┘");
        
        return sb.toString();
    }
    
    /**
     * Форматирует список файлов в таблицу
     */
    public static String formatFileList(List<FileEntity> files) {
        if (files == null || files.isEmpty()) {
            return "  (список пуст)";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("┌─────────────────────────────────────────────────────────────────────────────┐\n");
        sb.append("│ Список файлов                                                              │\n");
        sb.append("├─────────────────────────────────────────────────────────────────────────────┤\n");
        
        for (int i = 0; i < files.size(); i++) {
            FileEntity file = files.get(i);
            String name = truncate(file.getFilename(), 40);
            String size = formatFileSize(file.getSize() != null ? file.getSize() : 0);
            String date = formatDateTime(file.getCreatedAt());
            
            sb.append(String.format("│ %-40s │ %10s │ %-19s │\n", name, size, date));
            
            if (i < files.size() - 1) {
                sb.append("├─────────────────────────────────────────────────────────────────────────────┤\n");
            }
        }
        
        sb.append("└─────────────────────────────────────────────────────────────────────────────┘");
        return sb.toString();
    }
    
    /**
     * Форматирует простой список файлов (только имена)
     */
    public static String formatSimpleFileList(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return "  (директория пуста)";
        }
        
        StringBuilder sb = new StringBuilder();
        fileNames.forEach(fileName -> sb.append("  • ").append(fileName).append("\n"));
        return sb.toString().trim();
    }
    
    /**
     * Форматирует JSON/XML данные с отступами
     */
    public static String formatJsonXmlData(Object data) {
        if (data == null) {
            return "(пусто)";
        }
        return data.toString();
    }
    
    /**
     * Обрезает строку до указанной длины
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Создает разделительную линию
     */
    public static String createSeparator(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.max(0, length); i++) {
            sb.append("─");
        }
        return sb.toString();
    }
}

