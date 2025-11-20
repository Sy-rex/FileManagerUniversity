import zipfile

# Создаем ZIP с 10001 файлом
with zipfile.ZipFile('bomb_many_entries.zip', 'w') as zf:
    for i in range(10001):
        zf.writestr(f'file{i}.txt', 'test data')