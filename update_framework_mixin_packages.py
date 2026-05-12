import os

path = r'd:\Minecraft\projects\MyuLib-mc\myulib-framework\src\main\java\com\myudog\myulib\mixin\framework'
for filename in os.listdir(path):
    if filename.endswith('.java'):
        file_path = os.path.join(path, filename)
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        content = content.replace('package com.myudog.myulib.mixin;', 'package com.myudog.myulib.mixin.framework;')
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
print("Done updating package names.")
