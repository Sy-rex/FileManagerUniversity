"""
Скрипты для создания тестовых ZIP-бомб
ВНИМАНИЕ: Используйте только для тестирования защиты!
"""

import zipfile
import os

def create_zip_many_entries():
    """Создает ZIP с большим количеством записей (>10000)"""
    print("Создание ZIP с 10001 записью...")
    with zipfile.ZipFile('bomb_many_entries.zip', 'w', zipfile.ZIP_DEFLATED) as zf:
        for i in range(10001):
            zf.writestr(f'file{i}.txt', f'Test data {i}')
    print("✅ Создан bomb_many_entries.zip")
    print("   Попробуйте извлечь через приложение - должна быть ошибка")

def create_zip_high_compression():
    """Создает ZIP с высокой степенью сжатия"""
    print("Создание ZIP с высокой степенью сжатия...")
    # Создаем файл с повторяющимися данными (хорошо сжимается)
    data = b'0' * (10 * 1024 * 1024)  # 10MB нулей
    
    with zipfile.ZipFile('bomb_compression.zip', 'w', zipfile.ZIP_DEFLATED) as zf:
        # Создаем запись с заявленным размером 1GB, но реально сжимается до маленького
        info = zipfile.ZipInfo('bomb.txt')
        info.file_size = 1024 * 1024 * 1024  # Заявленный размер 1GB
        zf.writestr(info, data)
    
    print("✅ Создан bomb_compression.zip")
    print("   Попробуйте извлечь через приложение - должна быть ошибка")

def create_zip_large_size():
    """Создает ZIP с превышением максимального размера (>1GB)"""
    print("Создание ZIP с размером >1GB...")
    with zipfile.ZipFile('bomb_size.zip', 'w', zipfile.ZIP_STORED) as zf:
        # Создаем несколько файлов по 200MB каждый (сумма > 1GB)
        for i in range(6):  # 6 * 200MB = 1.2GB
            data = b'X' * (200 * 1024 * 1024)
            zf.writestr(f'file{i}.txt', data)
            print(f"   Добавлен file{i}.txt (200MB)")
    
    print("✅ Создан bomb_size.zip")
    print("   ВНИМАНИЕ: Файл будет большим (~1.2GB)!")
    print("   Попробуйте извлечь через приложение - должна быть ошибка")

def create_normal_zip():
    """Создает нормальный ZIP для проверки"""
    print("Создание нормального ZIP...")
    with zipfile.ZipFile('normal.zip', 'w', zipfile.ZIP_DEFLATED) as zf:
        for i in range(10):
            zf.writestr(f'file{i}.txt', f'Normal file content {i}')
    print("✅ Создан normal.zip")
    print("   Этот ZIP должен извлекаться без ошибок")

if __name__ == '__main__':
    print("=== Генератор тестовых ZIP-бомб ===\n")
    print("Выберите тип ZIP для создания:")
    print("1. ZIP с большим количеством записей (>10000)")
    print("2. ZIP с высокой степенью сжатия")
    print("3. ZIP с превышением максимального размера (>1GB)")
    print("4. Нормальный ZIP (для проверки)")
    print("5. Создать все тестовые файлы")
    
    choice = input("\nВаш выбор (1-5): ").strip()
    
    if choice == '1':
        create_zip_many_entries()
    elif choice == '2':
        create_zip_high_compression()
    elif choice == '3':
        create_zip_large_size()
    elif choice == '4':
        create_normal_zip()
    elif choice == '5':
        create_zip_many_entries()
        print()
        create_zip_high_compression()
        print()
        create_normal_zip()
        print("\n⚠️  Пропущен bomb_size.zip (слишком большой, создайте вручную если нужно)")
    else:
        print("Неверный выбор")

