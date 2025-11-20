import zipfile

with zipfile.ZipFile('bomb_size.zip', 'w') as zf:
    # Создаем несколько файлов по 200MB каждый (сумма > 1GB)
    for i in range(6):  # 6 * 200MB = 1.2GB
        data = b'X' * (200 * 1024 * 1024)
        zf.writestr(f'file{i}.txt', data)