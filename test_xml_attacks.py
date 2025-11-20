"""
Скрипты для создания тестовых XML файлов с атаками
ВНИМАНИЕ: Используйте только для тестирования защиты!
"""

def create_xxe_attack():
    """Создает XML файл с XXE атакой"""
    xxe_xml = '''<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<root>
  <data>&xxe;</data>
</root>'''
    
    with open('xxe_attack.xml', 'w', encoding='utf-8') as f:
        f.write(xxe_xml)
    
    print("✅ Создан xxe_attack.xml")
    print("   Попробуйте прочитать через приложение - должна быть ошибка о XXE атаке")

def create_billion_laughs():
    """Создает XML файл с Billion Laughs атакой"""
    billion_laughs = '''<?xml version="1.0"?>
<!DOCTYPE lolz [
  <!ENTITY lol "lol">
  <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
  <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
  <!ENTITY lol4 "&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;">
  <!ENTITY lol5 "&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;">
  <!ENTITY lol6 "&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;">
  <!ENTITY lol7 "&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;">
  <!ENTITY lol8 "&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;">
  <!ENTITY lol9 "&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;">
]>
<root>&lol9;</root>'''
    
    with open('billion_laughs.xml', 'w', encoding='utf-8') as f:
        f.write(billion_laughs)
    
    print("✅ Создан billion_laughs.xml")
    print("   Попробуйте прочитать через приложение - должна быть ошибка о Billion Laughs")

def create_deep_nested_xml(depth=150):
    """Создает XML с глубокой вложенностью"""
    xml_str = '<?xml version="1.0"?>\n<root>\n'
    
    indent = "  "
    for i in range(depth):
        xml_str += indent * (i + 1) + f'<level{i}>\n'
    
    # Закрываем все теги
    for i in range(depth - 1, -1, -1):
        xml_str += indent * (i + 1) + f'</level{i}>\n'
    
    xml_str += '</root>'
    
    with open('deep_nested_xml.xml', 'w', encoding='utf-8') as f:
        f.write(xml_str)
    
    print(f"✅ Создан deep_nested_xml.xml с {depth} уровнями вложенности")
    print("   Попробуйте прочитать через приложение - должна быть ошибка о превышении глубины")

def create_normal_xml():
    """Создает нормальный XML файл"""
    normal_xml = '''<?xml version="1.0"?>
<root>
  <user>
    <name>Test User</name>
    <email>test@example.com</email>
  </user>
  <data>
    <item>Item 1</item>
    <item>Item 2</item>
  </data>
</root>'''
    
    with open('normal.xml', 'w', encoding='utf-8') as f:
        f.write(normal_xml)
    
    print("✅ Создан normal.xml")
    print("   Этот XML должен читаться без ошибок")

if __name__ == '__main__':
    print("=== Генератор тестовых XML атак ===\n")
    print("Выберите тип XML для создания:")
    print("1. XXE атака (внешние сущности)")
    print("2. Billion Laughs атака")
    print("3. Глубокая вложенность (150 уровней)")
    print("4. Нормальный XML (для проверки)")
    print("5. Создать все тестовые файлы")
    
    choice = input("\nВаш выбор (1-5): ").strip()
    
    if choice == '1':
        create_xxe_attack()
    elif choice == '2':
        create_billion_laughs()
    elif choice == '3':
        create_deep_nested_xml()
    elif choice == '4':
        create_normal_xml()
    elif choice == '5':
        create_xxe_attack()
        print()
        create_billion_laughs()
        print()
        create_deep_nested_xml()
        print()
        create_normal_xml()
    else:
        print("Неверный выбор")

