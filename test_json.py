import json

data = {}
current = data
for i in range(1000):
    current[f'level{i}'] = {}
    current = current[f'level{i}']

with open('deep_nested.json', 'w') as f:
    json.dump(data, f)