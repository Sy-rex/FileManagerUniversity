package com.sobolev.spring.filemanageruniversity.console;

import com.sobolev.spring.filemanageruniversity.entity.FileEntity;
import com.sobolev.spring.filemanageruniversity.entity.OperationType;
import com.sobolev.spring.filemanageruniversity.entity.User;
import com.sobolev.spring.filemanageruniversity.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Component
public class ConsoleInterface implements CommandLineRunner {

    private final UserService userService;
    private final FileService fileService;
    private final JsonXmlService jsonXmlService;
    private final ZipService zipService;
    private final DiskService diskService;
    private final SecurityService securityService;
    private final AuditService auditService;

    private User currentUser;
    private Scanner scanner;

    @Autowired
    public ConsoleInterface(UserService userService, FileService fileService,
                           JsonXmlService jsonXmlService, ZipService zipService,
                           DiskService diskService, SecurityService securityService,
                           AuditService auditService) {
        this.userService = userService;
        this.fileService = fileService;
        this.jsonXmlService = jsonXmlService;
        this.zipService = zipService;
        this.diskService = diskService;
        this.securityService = securityService;
        this.auditService = auditService;
    }

    @Override
    public void run(String... args) throws Exception {
        scanner = new Scanner(System.in);

        securityService.ensureBaseDirectoryExists();
        
        System.out.println("=== Безопасный файловый менеджер ===");
        System.out.println("Базовая директория: " + securityService.getBaseDirectory().toAbsolutePath());
        System.out.println("(Все пути должны быть относительными к базовой директории)\n");

        authenticateUser();
        
        if (currentUser != null) {
            showMainMenu();
        }
    }

    private void authenticateUser() {
        while (currentUser == null) {
            System.out.println("1. Войти");
            System.out.println("2. Зарегистрироваться");
            System.out.print("Выберите действие: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    login();
                    break;
                case "2":
                    register();
                    break;
                default:
                    System.out.println("Неверный выбор. Попробуйте снова.\n");
            }
        }
    }

    private void login() {
        System.out.print("Введите имя пользователя: ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine();
        
        try {
            var userOpt = userService.authenticateUser(username, password);
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                System.out.println("Успешный вход! Добро пожаловать, " + currentUser.getUsername() + "!\n");
                auditService.logOperation(currentUser, OperationType.READ, "Вход в систему");
            } else {
                System.out.println("Неверное имя пользователя или пароль.\n");
            }
        } catch (Exception e) {
            System.out.println("Ошибка при входе: " + e.getMessage() + "\n");
        }
    }

    private void register() {
        System.out.print("Введите имя пользователя (3-50 символов): ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine();
        
        try {
            User newUser = userService.registerUser(username, password);
            currentUser = newUser;
            System.out.println("Регистрация успешна! Добро пожаловать, " + currentUser.getUsername() + "!\n");
            auditService.logOperation(currentUser, OperationType.CREATE, "Регистрация нового пользователя");
        } catch (Exception e) {
            System.out.println("Ошибка при регистрации: " + e.getMessage() + "\n");
        }
    }

    /**
     * Главное меню
     */
    private void showMainMenu() {
        while (true) {
            System.out.println("\n=== Главное меню ===");
            System.out.println("1. Информация о дисках");
            System.out.println("2. Работа с файлами");
            System.out.println("3. Работа с JSON/XML");
            System.out.println("4. Работа с ZIP архивами");
            System.out.println("5. Мои файлы");
            System.out.println("6. Выход");
            System.out.print("Выберите действие: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    showDisksInfo();
                    break;
                case "2":
                    showFileMenu();
                    break;
                case "3":
                    showJsonXmlMenu();
                    break;
                case "4":
                    showZipMenu();
                    break;
                case "5":
                    showUserFiles();
                    break;
                case "6":
                    System.out.println("До свидания!");
                    return;
                default:
                    System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }

    private void showDisksInfo() {
        System.out.println("\n" + diskService.getDisksInfo());
    }

    private void showFileMenu() {
        while (true) {
            System.out.println("\n=== Работа с файлами ===");
            System.out.println("1. Прочитать файл");
            System.out.println("2. Создать/Изменить файл");
            System.out.println("3. Удалить файл");
            System.out.println("4. Копировать файл");
            System.out.println("5. Переместить файл");
            System.out.println("6. Список файлов в директории");
            System.out.println("7. Информация о файле");
            System.out.println("8. Назад");
            System.out.print("Выберите действие: ");
            
            String choice = scanner.nextLine().trim();
            
            try {
                switch (choice) {
                    case "1":
                        readFile();
                        break;
                    case "2":
                        writeFile();
                        break;
                    case "3":
                        deleteFile();
                        break;
                    case "4":
                        copyFile();
                        break;
                    case "5":
                        moveFile();
                        break;
                    case "6":
                        listFiles();
                        break;
                    case "7":
                        getFileInfo();
                        break;
                    case "8":
                        return;
                    default:
                        System.out.println("Неверный выбор.");
                }
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void readFile() throws IOException {
        System.out.print("Введите путь к файлу (относительный, например: test.txt): ");
        String filePath = scanner.nextLine().trim();
        if (filePath.isEmpty()) {
            System.out.println("Путь не может быть пустым.");
            return;
        }
        String content = fileService.readFile(filePath, currentUser);
        System.out.println("\nСодержимое файла:\n" + content);
    }

    private void writeFile() throws IOException {
        System.out.print("Введите путь к файлу (относительный, например: test.txt или folder/file.txt): ");
        String filePath = scanner.nextLine().trim();
        if (filePath.isEmpty()) {
            System.out.println("Путь не может быть пустым.");
            return;
        }
        System.out.println("Введите содержимое файла (для завершения введите пустую строку):");
        StringBuilder content = new StringBuilder();
        String line;
        while (!(line = scanner.nextLine()).isEmpty()) {
            content.append(line).append("\n");
        }
        fileService.writeFile(filePath, content.toString(), currentUser);
        System.out.println("Файл успешно сохранен.");
    }

    private void deleteFile() {
        try {
            System.out.print("Введите путь к файлу для удаления (относительный, например: test.txt): ");
            String filePath = scanner.nextLine().trim();
            if (filePath.isEmpty()) {
                System.out.println("Путь не может быть пустым.");
                return;
            }
            System.out.print("Вы уверены? (yes/no): ");
            String confirm = scanner.nextLine().trim();
            if ("yes".equalsIgnoreCase(confirm)) {
                fileService.deleteFile(filePath, currentUser);
                System.out.println("Файл успешно удален.");
            } else {
                System.out.println("Операция отменена.");
            }
        } catch (Exception e) {
            System.out.println("Ошибка при удалении файла: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            if (e.getCause() != null) {
                System.out.println("Причина: " + e.getCause().getMessage());
            }
        }
    }

    private void copyFile() throws IOException {
        System.out.print("Введите путь к исходному файлу: ");
        String sourcePath = scanner.nextLine().trim();
        System.out.print("Введите путь к файлу назначения: ");
        String destPath = scanner.nextLine().trim();
        fileService.copyFile(sourcePath, destPath, currentUser);
        System.out.println("Файл успешно скопирован.");
    }

    private void moveFile() throws IOException {
        System.out.print("Введите путь к исходному файлу: ");
        String sourcePath = scanner.nextLine().trim();
        System.out.print("Введите путь к файлу назначения: ");
        String destPath = scanner.nextLine().trim();
        fileService.moveFile(sourcePath, destPath, currentUser);
        System.out.println("Файл успешно перемещен.");
    }

    private void listFiles() throws IOException {
        System.out.print("Введите путь к директории (относительный, для корня введите . или пустую строку): ");
        String dirPath = scanner.nextLine().trim();
        if (dirPath.isEmpty()) {
            dirPath = "."; // Текущая директория (корень базовой директории)
        }
        List<String> files = fileService.listFiles(dirPath, currentUser);
        System.out.println("\nФайлы в директории:");
        if (files.isEmpty()) {
            System.out.println("  (директория пуста)");
        } else {
            files.forEach(file -> System.out.println("  - " + file));
        }
    }

    private void getFileInfo() {
        System.out.print("Введите путь к файлу: ");
        String filePath = scanner.nextLine().trim();
        FileEntity fileInfo = fileService.getFileInfo(filePath, currentUser);
        if (fileInfo != null) {
            System.out.println("\nИнформация о файле:");
            System.out.println("Имя: " + fileInfo.getFilename());
            System.out.println("Размер: " + fileInfo.getSize() + " байт");
            System.out.println("Дата создания: " + fileInfo.getCreatedAt());
            System.out.println("Тип: " + fileInfo.getFileType());
            System.out.println("Архивирован: " + fileInfo.getIsArchived());
        } else {
            System.out.println("Файл не найден в базе данных.");
        }
    }

    /**
     * Меню работы с JSON/XML
     */
    private void showJsonXmlMenu() {
        while (true) {
            System.out.println("\n=== Работа с JSON/XML ===");
            System.out.println("1. Прочитать JSON файл");
            System.out.println("2. Записать JSON файл");
            System.out.println("3. Прочитать XML файл");
            System.out.println("4. Записать XML файл");
            System.out.println("5. Назад");
            System.out.print("Выберите действие: ");
            
            String choice = scanner.nextLine().trim();
            
            try {
                switch (choice) {
                    case "1":
                        readJsonFile();
                        break;
                    case "2":
                        writeJsonFile();
                        break;
                    case "3":
                        readXmlFile();
                        break;
                    case "4":
                        writeXmlFile();
                        break;
                    case "5":
                        return;
                    default:
                        System.out.println("Неверный выбор.");
                }
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void readJsonFile() throws IOException {
        System.out.print("Введите путь к JSON файлу: ");
        String filePath = scanner.nextLine().trim();
        Map<String, Object> data = jsonXmlService.readJsonFile(filePath);
        System.out.println("\nСодержимое JSON файла:");
        System.out.println(data);
    }

    private void writeJsonFile() throws IOException {
        System.out.print("Введите путь к JSON файлу: ");
        String filePath = scanner.nextLine().trim();
        System.out.println("Введите JSON данные в формате ключ=значение (для завершения введите пустую строку):");
        Map<String, Object> data = new java.util.HashMap<>();
        String line;
        while (!(line = scanner.nextLine()).isEmpty()) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                data.put(parts[0].trim(), parts[1].trim());
            }
        }
        jsonXmlService.writeJsonFile(filePath, data);
        System.out.println("JSON файл успешно сохранен.");
    }

    private void readXmlFile() throws IOException {
        System.out.print("Введите путь к XML файлу: ");
        String filePath = scanner.nextLine().trim();
        Map<String, Object> data = jsonXmlService.readXmlFile(filePath);
        System.out.println("\nСодержимое XML файла:");
        System.out.println(data);
    }

    private void writeXmlFile() throws IOException {
        System.out.print("Введите путь к XML файлу: ");
        String filePath = scanner.nextLine().trim();
        System.out.println("Введите XML данные в формате ключ=значение (для завершения введите пустую строку):");
        Map<String, Object> data = new java.util.HashMap<>();
        String line;
        while (!(line = scanner.nextLine()).isEmpty()) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                data.put(parts[0].trim(), parts[1].trim());
            }
        }
        jsonXmlService.writeXmlFile(filePath, data);
        System.out.println("XML файл успешно сохранен.");
    }

    private void showZipMenu() {
        while (true) {
            System.out.println("\n=== Работа с ZIP архивами ===");
            System.out.println("1. Создать ZIP архив");
            System.out.println("2. Извлечь ZIP архив");
            System.out.println("3. Назад");
            System.out.print("Выберите действие: ");
            
            String choice = scanner.nextLine().trim();
            
            try {
                switch (choice) {
                    case "1":
                        createZipArchive();
                        break;
                    case "2":
                        extractZipArchive();
                        break;
                    case "3":
                        return;
                    default:
                        System.out.println("Неверный выбор.");
                }
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void createZipArchive() throws IOException {
        System.out.print("Введите путь к ZIP архиву: ");
        String zipPath = scanner.nextLine().trim();
        System.out.print("Введите пути к файлам для архивации (через пробел): ");
        String filesInput = scanner.nextLine().trim();
        String[] filePaths = filesInput.split("\\s+");
        zipService.createZipArchive(zipPath, filePaths);
        System.out.println("ZIP архив успешно создан.");
    }

    private void extractZipArchive() throws IOException {
        System.out.print("Введите путь к ZIP архиву: ");
        String zipPath = scanner.nextLine().trim();
        System.out.print("Введите путь для извлечения: ");
        String extractPath = scanner.nextLine().trim();
        zipService.extractZipArchive(zipPath, extractPath);
        System.out.println("ZIP архив успешно извлечен.");
    }

    private void showUserFiles() {
        List<FileEntity> files = fileService.getUserFiles(currentUser);
        if (files.isEmpty()) {
            System.out.println("\nУ вас пока нет файлов.");
        } else {
            System.out.println("\nВаши файлы:");
            files.forEach(file -> {
                System.out.println("  - " + file.getFilename() + " (" + file.getSize() + " байт, " + file.getCreatedAt() + ")");
            });
        }
    }
}
