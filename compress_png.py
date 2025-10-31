#!/usr/bin/env python3
from PIL import Image
import os
import glob

# 在icon目录下找所有大于20KB的PNG
icon_dir = "/d/4_code/qiandoudou/qiandoudou-frontend/static/icon"

def compress_image(input_path):
    """压缩单个图片"""
    try:
        original_size = os.path.getsize(input_path) / 1024
        img = Image.open(input_path)

        # 如果宽度超过800px，压缩
        if img.width > 800:
            ratio = 800 / img.width
            new_height = int(img.height * ratio)
            img = img.resize((800, new_height), Image.Resampling.LANCZOS)

        # 转换为RGB后保存为PNG（去掉透明度降低体积）
        if img.mode in ('RGBA', 'LA', 'P'):
            # 保留透明度
            img.save(input_path, 'PNG', optimize=True)
        else:
            img.save(input_path, 'PNG', optimize=True, quality=90)

        new_size = os.path.getsize(input_path) / 1024
        reduction = original_size - new_size
        pct = (reduction / original_size * 100) if original_size > 0 else 0

        print(f"✅ {os.path.relpath(input_path, icon_dir)}")
        print(f"   {original_size:.1f}KB → {new_size:.1f}KB (-{reduction:.1f}KB, {pct:.1f}%)")

    except Exception as e:
        print(f"❌ {os.path.basename(input_path)}: {e}")

print("压缩所有PNG文件...\n")

# 查找所有PNG
for png_file in glob.glob(os.path.join(icon_dir, "**/*.png"), recursive=True):
    size_kb = os.path.getsize(png_file) / 1024
    if size_kb > 20:  # 只压缩超过20KB的文件
        compress_image(png_file)

print("\n✨ 完成！")
