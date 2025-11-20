"""
Скрипт для создания JSON файла с глубокой вложенностью
для тестирования защиты от небезопасной десериализации
"""

# Создаем JSON напрямую как строку, чтобы избежать RecursionError в Python
def create_deep_nested_json(depth=150):
    """
    Создает JSON с указанной глубиной вложенности
    depth: количество уровней вложенности (рекомендуется 100-150 для теста)
    """
    json_str = "{"
    
    for i in range(depth):
        json_str += f'"level{i}": {{'
    
    # Закрываем все скобки
    json_str += "}" * depth
    
    return json_str

# Создаем JSON с 150 уровнями вложенности (больше чем лимит в 100)
nested_json = create_deep_nested_json(150)

# Сохраняем в файл
with open('deep_nested.json', 'w', encoding='utf-8') as f:
    f.write(nested_json)

print(f"✅ Создан файл deep_nested.json с 150 уровнями вложенности")
print(f"   Размер файла: {len(nested_json)} байт")
print(f"   Попробуйте прочитать его через приложение - должна быть ошибка о превышении глубины")