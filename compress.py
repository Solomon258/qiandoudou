#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
import os

# 强制使用UTF-8输出
if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

from PIL import Image

images = [
    r'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon\sel-car.png',
    r'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon\travel.png',
    r'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon\dazi.png',
    r'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon\qinglv.png',
    r'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon\gerenzan.png',
    r'https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon\paopao\sini.png',
]

print("开始压缩图片...")
total_saved = 0

for img_path in images:
    if not os.path.exists(img_path):
        print(f"警告: 不存在: {img_path}")
        continue

    try:
        orig_size = os.path.getsize(img_path) / 1024
        img = Image.open(img_path)

        # 缩放到最大宽度 750px
        if img.width > 750:
            ratio = 750 / img.width
            new_h = int(img.height * ratio)
            img = img.resize((750, new_h), Image.Resampling.LANCZOS)

        # 优化保存
        img.save(img_path, 'PNG', optimize=True)
        new_size = os.path.getsize(img_path) / 1024
        saved = orig_size - new_size
        total_saved += saved

        print(f"成功: {os.path.basename(img_path)}")
        print(f"   {orig_size:.1f}KB -> {new_size:.1f}KB (节省{saved:.1f}KB)")
    except Exception as e:
        print(f"失败: {os.path.basename(img_path)} - {e}")

print(f"\n总共节省: {total_saved:.1f}KB")
