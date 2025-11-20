package com.sobolev.spring.filemanageruniversity.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sobolev.spring.filemanageruniversity.config.FileManagerConstants;
import com.sobolev.spring.filemanageruniversity.exception.FileNotFoundException;
import com.sobolev.spring.filemanageruniversity.exception.XmlSecurityException;
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
        
        // Настройка JSON mapper с защитой от небезопасной десериализации
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        // Ограничение глубины вложенности для защиты от StackOverflow
        this.jsonMapper.getFactory().setStreamReadConstraints(
            com.fasterxml.jackson.core.StreamReadConstraints.builder()
                .maxNestingDepth(100) // Максимальная глубина вложенности
                .build()
        );
        
        // Настройка XML mapper с защитой от XXE и других атак
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        // Jackson XML по умолчанию не обрабатывает DTD, что защищает от XXE
        // Но добавим явную защиту через настройки парсера
        this.xmlMapper.getFactory().getXMLInputFactory().setProperty(
            "javax.xml.stream.isSupportingExternalEntities", false
        );
        this.xmlMapper.getFactory().getXMLInputFactory().setProperty(
            "javax.xml.stream.supportDTD", false
        );
        // Ограничение глубины вложенности
        this.xmlMapper.getFactory().setStreamReadConstraints(
            com.fasterxml.jackson.core.StreamReadConstraints.builder()
                .maxNestingDepth(100) // Максимальная глубина вложенности
                .build()
        );
    }

    public Map<String, Object> readJsonFile(String filePath) throws IOException {
        Path validatedPath = securityService.validateAndNormalizePath(filePath);
        File file = validatedPath.toFile();
        
        if (!file.exists()) {
            throw new FileNotFoundException(filePath);
        }
        
        securityService.validateFileSize(file.length());

        try {
            return jsonMapper.readValue(file, Map.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Проверяем на признаки атак
            String message = e.getMessage();
            if (message != null && (message.contains("nesting") || message.contains("depth"))) {
                throw new XmlSecurityException("Превышена максимальная глубина вложенности JSON", e);
            }
            throw e;
        }
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
            throw new FileNotFoundException(filePath);
        }
        
        securityService.validateFileSize(file.length());
        
        try {
            // Безопасная десериализация в Map
            return xmlMapper.readValue(file, Map.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Обработка XML-специфичных ошибок безопасности
            handleXmlSecurityException(e);
            throw e;
        } catch (Exception e) {
            // Обработка всех остальных исключений при парсинге XML
            handleXmlSecurityException(e);
            throw new IOException("Ошибка при чтении XML файла: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверяет исключение на признаки XML атак и выбрасывает XmlSecurityException
     */
    private void handleXmlSecurityException(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Проверка на XXE атаки (DTD, внешние сущности)
        if (lowerMessage.contains("undeclared general entity") || 
            lowerMessage.contains("entity") && lowerMessage.contains("dtd") ||
            lowerMessage.contains("doctype") ||
            lowerMessage.contains("external entity") ||
            lowerMessage.contains("entity reference") ||
            (lowerMessage.contains("entity") && lowerMessage.contains("declared"))) {
            throw new XmlSecurityException(
                "Обнаружена попытка использования внешних сущностей (XXE атака). " +
                "DTD и внешние сущности не поддерживаются из соображений безопасности.", 
                e
            );
        }
        
        // Проверка на Billion Laughs атаку (глубокая вложенность)
        if (lowerMessage.contains("nesting") || 
            lowerMessage.contains("depth") || 
            lowerMessage.contains("recursive") ||
            lowerMessage.contains("too deep")) {
            throw new XmlSecurityException(
                "Превышена максимальная глубина вложенности XML. " +
                "Возможна попытка Billion Laughs атаки.", 
                e
            );
        }
        
        // Общая проверка на DTD и сущности
        if ((lowerMessage.contains("entity") || lowerMessage.contains("dtd")) && 
            (lowerMessage.contains("not") || lowerMessage.contains("unsupported") || 
             lowerMessage.contains("declared"))) {
            throw new XmlSecurityException(
                "Обнаружена потенциальная XML атака. " +
                "Файл содержит небезопасные конструкции (DTD, внешние сущности).", 
                e
            );
        }
    }

    public void writeXmlFile(String filePath, Map<String, Object> data) throws IOException {
        Path validatedPath = securityService.validateAndNormalizePath(filePath);
        
        // Создаем директорию, если не существует
        Files.createDirectories(validatedPath.getParent());
        
        // Безопасная сериализация
        xmlMapper.writerWithDefaultPrettyPrinter().writeValue(validatedPath.toFile(), data);
    }
}

