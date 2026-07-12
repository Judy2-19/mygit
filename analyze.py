from pathlib import Path

lines = Path('src/GamePanel.java').read_text(encoding='utf-8').splitlines()

for i in range(904, 926):
    l = lines[i]
    print(f'{i+1:4d}: {repr(l)}')




