package com.sobolev.spring.filemanageruniversity.console;

import com.sobolev.spring.filemanageruniversity.entity.FileEntity;
import com.sobolev.spring.filemanageruniversity.entity.OperationType;
import com.sobolev.spring.filemanageruniversity.entity.User;
import com.sobolev.spring.filemanageruniversity.exception.*;
import com.sobolev.spring.filemanageruniversity.service.*;
import com.sobolev.spring.filemanageruniversity.util.InputValidator;
import com.sobolev.spring.filemanageruniversity.util.OutputFormatter;
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
        } catch (ValidationException e) {
            System.out.println("⚠️  " + e.getMessage() + "\n");
        } catch (Exception e) {
            handleException(e, "Ошибка при входе");
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
        } catch (ValidationException e) {
            System.out.println("⚠️  " + e.getMessage() + "\n");
        } catch (Exception e) {
            handleException(e, "Ошибка при регистрации");
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
                handleFileOperationException(e, "выполнении операции");
            }
        }
    }

    private void readFile() {
        try {
            String filePath = readInputPath(
                "Введите путь к файлу (относительный, например: test.txt): ",
                "test.txt"
            );
            if (filePath == null) return;
            
            String content = fileService.readFile(filePath, currentUser);
            System.out.println("\n📄 Содержимое файла:");
            System.out.println(OutputFormatter.createSeparator(60));
            System.out.println(content);
            System.out.println(OutputFormatter.createSeparator(60));
        } catch (Exception e) {
            handleFileOperationException(e, "чтении файла");
        }
    }

    private void writeFile() {
        try {
            String filePath = readInputPath(
                "Введите путь к файлу (относительный, например: test.txt или folder/file.txt): ",
                "test.txt"
            );
            if (filePath == null) return;
            
            System.out.println("Введите содержимое файла (для завершения введите пустую строку):");
            StringBuilder content = new StringBuilder();
            String line;
            while (!(line = scanner.nextLine()).isEmpty()) {
                content.append(line).append("\n");
            }
            
            if (content.length() == 0) {
                System.out.println("⚠️  Предупреждение: файл будет пустым.");
            }
            
            fileService.writeFile(filePath, content.toString(), currentUser);
            System.out.println("✅ Файл успешно сохранен: " + filePath);
        } catch (Exception e) {
            handleFileOperationException(e, "записи файла");
        }
    }

    private void deleteFile() {
        try {
            String filePath = readInputPath(
                "Введите путь к файлу для удаления (относительный, например: test.txt): ",
                "test.txt"
            );
            if (filePath == null) return;
            
            System.out.print("⚠️  Вы уверены, что хотите удалить файл? (yes/no): ");
            String confirm = scanner.nextLine().trim();
            if ("yes".equalsIgnoreCase(confirm)) {
                fileService.deleteFile(filePath, currentUser);
                System.out.println("✅ Файл успешно удален: " + filePath);
            } else {
                System.out.println("❌ Операция отменена.");
            }
        } catch (Exception e) {
            handleFileOperationException(e, "удалении файла");
        }
    }

    private void copyFile() {
        try {
            String sourcePath = readInputPath(
                "Введите путь к исходному файлу: ",
                "source.txt"
            );
            if (sourcePath == null) return;
            
            String destPath = readInputPath(
                "Введите путь к файлу назначения: ",
                "destination.txt"
            );
            if (destPath == null) return;
            
            fileService.copyFile(sourcePath, destPath, currentUser);
            System.out.println("✅ Файл успешно скопирован из " + sourcePath + " в " + destPath);
        } catch (Exception e) {
            handleFileOperationException(e, "копировании файла");
        }
    }

    private void moveFile() {
        try {
            String sourcePath = readInputPath(
                "Введите путь к исходному файлу: ",
                "source.txt"
            );
            if (sourcePath == null) return;
            
            String destPath = readInputPath(
                "Введите путь к файлу назначения: ",
                "destination.txt"
            );
            if (destPath == null) return;
            
            fileService.moveFile(sourcePath, destPath, currentUser);
            System.out.println("✅ Файл успешно перемещен из " + sourcePath + " в " + destPath);
        } catch (Exception e) {
            handleFileOperationException(e, "перемещении файла");
        }
    }

    private void listFiles() {
        try {
            System.out.print("Введите путь к директории (относительный, для корня введите . или пустую строку): ");
            String dirPath = scanner.nextLine().trim();
            if (dirPath.isEmpty()) {
                dirPath = "."; // Текущая директория (корень базовой директории)
            }
            
            List<String> files = fileService.listFiles(dirPath, currentUser);
            System.out.println("\n📁 Файлы в директории (" + dirPath + "):");
            System.out.println(OutputFormatter.formatSimpleFileList(files));
        } catch (Exception e) {
            handleFileOperationException(e, "получении списка файлов");
        }
    }

    private void getFileInfo() {
        try {
            String filePath = readInputPath(
                "Введите путь к файлу: ",
                "test.txt"
            );
            if (filePath == null) return;
            
            FileEntity fileInfo = fileService.getFileInfo(filePath, currentUser);
            if (fileInfo != null) {
                System.out.println("\n" + OutputFormatter.formatFileInfo(fileInfo));
            } else {
                System.out.println("❌ Файл не найден в базе данных: " + filePath);
            }
        } catch (Exception e) {
            handleFileOperationException(e, "получении информации о файле");
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
                handleFileOperationException(e, "выполнении операции");
            }
        }
    }

    private void readJsonFile() {
        try {
            String filePath = readInputPath(
                "Введите путь к JSON файлу: ",
                "data.json"
            );
            if (filePath == null) return;
            
            Map<String, Object> data = jsonXmlService.readJsonFile(filePath);
            System.out.println("\n📄 Содержимое JSON файла:");
            System.out.println(OutputFormatter.formatJsonXmlData(data));
        } catch (Exception e) {
            handleFileOperationException(e, "чтении JSON файла");
        }
    }

    private void writeJsonFile() {
        try {
            String filePath = readInputPath(
                "Введите путь к JSON файлу: ",
                "data.json"
            );
            if (filePath == null) return;
            
            System.out.println("Введите JSON данные в формате ключ=значение (для завершения введите пустую строку):");
            Map<String, Object> data = new java.util.HashMap<>();
            String line;
            while (!(line = scanner.nextLine()).isEmpty()) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    data.put(parts[0].trim(), parts[1].trim());
                } else {
                    System.out.println("⚠️  Неверный формат. Используйте: ключ=значение");
                }
            }
            
            if (data.isEmpty()) {
                System.out.println("⚠️  Предупреждение: файл будет пустым.");
            }
            
            jsonXmlService.writeJsonFile(filePath, data);
            System.out.println("✅ JSON файл успешно сохранен: " + filePath);
        } catch (Exception e) {
            handleFileOperationException(e, "записи JSON файла");
        }
    }

    private void readXmlFile() {
        try {
            String filePath = readInputPath(
                "Введите путь к XML файлу: ",
                "data.xml"
            );
            if (filePath == null) return;
            
            Map<String, Object> data = jsonXmlService.readXmlFile(filePath);
            System.out.println("\n📄 Содержимое XML файла:");
            System.out.println(OutputFormatter.formatJsonXmlData(data));
        } catch (Exception e) {
            handleFileOperationException(e, "чтении XML файла");
        }
    }

    private void writeXmlFile() {
        try {
            String filePath = readInputPath(
                "Введите путь к XML файлу: ",
                "data.xml"
            );
            if (filePath == null) return;
            
            System.out.println("Введите XML данные в формате ключ=значение (для завершения введите пустую строку):");
            Map<String, Object> data = new java.util.HashMap<>();
            String line;
            while (!(line = scanner.nextLine()).isEmpty()) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    data.put(parts[0].trim(), parts[1].trim());
                } else {
                    System.out.println("⚠️  Неверный формат. Используйте: ключ=значение");
                }
            }
            
            if (data.isEmpty()) {
                System.out.println("⚠️  Предупреждение: файл будет пустым.");
            }
            
            jsonXmlService.writeXmlFile(filePath, data);
            System.out.println("✅ XML файл успешно сохранен: " + filePath);
        } catch (Exception e) {
            handleFileOperationException(e, "записи XML файла");
        }
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
                handleFileOperationException(e, "выполнении операции");
            }
        }
    }

    private void createZipArchive() {
        try {
            String zipPath = readInputPath(
                "Введите путь к ZIP архиву: ",
                "archive.zip"
            );
            if (zipPath == null) return;
            
            System.out.print("Введите пути к файлам для архивации (через пробел): ");
            String filesInput = scanner.nextLine().trim();
            if (filesInput.isEmpty()) {
                System.out.println("❌ Необходимо указать хотя бы один файл для архивации.");
                return;
            }
            
            String[] filePaths = filesInput.split("\\s+");
            zipService.createZipArchive(zipPath, filePaths);
            System.out.println("✅ ZIP архив успешно создан: " + zipPath);
        } catch (Exception e) {
            handleFileOperationException(e, "создании ZIP архива");
        }
    }

    private void extractZipArchive() {
        try {
            String zipPath = readInputPath(
                "Введите путь к ZIP архиву: ",
                "archive.zip"
            );
            if (zipPath == null) return;
            
            String extractPath = readInputPath(
                "Введите путь для извлечения: ",
                "extracted/"
            );
            if (extractPath == null) return;
            
            zipService.extractZipArchive(zipPath, extractPath);
            System.out.println("✅ ZIP архив успешно извлечен в: " + extractPath);
        } catch (Exception e) {
            handleFileOperationException(e, "извлечении ZIP архива");
        }
    }

    private void showUserFiles() {
        try {
            List<FileEntity> files = fileService.getUserFiles(currentUser);
            if (files.isEmpty()) {
                System.out.println("\nУ вас пока нет файлов.");
            } else {
                System.out.println("\n" + OutputFormatter.formatFileList(files));
            }
        } catch (Exception e) {
            handleException(e, "Ошибка при получении списка файлов");
        }
    }
    
    // ==================== Вспомогательные методы ====================
    
    /**
     * Читает и валидирует путь к файлу
     */
    private String readInputPath(String prompt, String example) {
        System.out.print(prompt);
        String path = scanner.nextLine().trim();
        try {
            return InputValidator.validatePath(path, "Путь", example);
        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Обрабатывает исключения с понятными сообщениями
     */
    private void handleException(Exception e, String context) {
        if (e instanceof FileNotFoundException) {
            FileNotFoundException fnfe = (FileNotFoundException) e;
            System.out.println("❌ Файл не найден: " + fnfe.getFilePath());
            System.out.println("   Убедитесь, что путь указан правильно и файл существует.");
        } else if (e instanceof SecurityException) {
            System.out.println("🔒 Ошибка безопасности: " + e.getMessage());
            System.out.println("   Операция отклонена из соображений безопасности.");
        } else if (e instanceof ValidationException) {
            System.out.println("⚠️  Ошибка валидации: " + e.getMessage());
        } else if (e instanceof ZipBombException) {
            System.out.println("💣 " + e.getMessage());
            System.out.println("   Архив заблокирован из соображений безопасности.");
        } else if (e instanceof IOException) {
            System.out.println("📁 Ошибка ввода-вывода: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("   Причина: " + e.getCause().getMessage());
            }
        } else {
            System.out.println("❌ " + context + ": " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("   Причина: " + e.getCause().getMessage());
            }
        }
        System.out.println();
    }
    
    /**
     * Обрабатывает исключения в контексте операции с файлами
     */
    private void handleFileOperationException(Exception e, String operation) {
        handleException(e, "Ошибка при " + operation);
    }
}
