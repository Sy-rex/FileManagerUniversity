package com.sobolev.spring.filemanageruniversity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class JsonXmlService {

    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;
    private final SecurityService securityService;

    @Autowired
    public JsonXmlService(SecurityService securityService) {
        this.securityService = securityService;
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public Map<String, Object> readJsonFile(String filePath) throws IOException {
        Path validatedPath = securityService.validateAndNormalizePath(filePath);
        File file = validatedPath.toFile();
        
        if (!file.exists()) {
            throw new IOException("Файл не найден: " + filePath);
        }
        
        securityService.validateFileSize(file.length());

        return jsonMapper.readValue(file, Map.class);
    }

    public void writeJsonFile(String filePath, Map<String, Object> data) throws IOException {
        Path validatedPath = securityService.validateAndNormalizePath(filePath);
        
        // Создаем директорию, если не существует
        Files.createDirectories(validatedPath.getParent());
        
        // Безопасная сериализация
        jsonMapper.writerWithDefaultPrettyPrinter().writeValue(validatedPath.toFile(), data);
    }

    public Map<String, Object> readXmlFile(String filePath) throws IOException {
        Path validatedPath = securityService.validateAndNormalizePath(filePath);
        File file = validatedPath.toFile();
        
        if (!file.exists()) {
            throw new IOException("Файл не найден: " + filePath);
        }
        
        securityService.validateFileSize(file.length());
        
        // Безопасная десериализация в Map
        return xmlMapper.readValue(file, Map.class);
    }

    public void writeXmlFile(String filePath, Map<String, Object> data) throws IOException {
        Path validatedPath = securityService.validateAndNormalizePath(filePath);
        
        // Создаем директорию, если не существует
        Files.createDirectories(validatedPath.getParent());
        
        // Безопасная сериализация
        xmlMapper.writerWithDefaultPrettyPrinter().writeValue(validatedPath.toFile(), data);
    }
}

