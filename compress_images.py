#!/usr/bin/env python3
from PIL import Image
import os

# 需要压缩的图片列表
images_to_compress = [
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon/paopao.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon/sel-car.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon/travel.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon/sini.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon/qinglv.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon/dazi.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon/time_changel.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon/dream_child.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon/gerenzan.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon/share_friend_circle2.png",
    "https://qiandoudou.oss-cn-guangzhou.aliyuncs.com/res/image/icon/video_choose_bg.png",
]

def compress_image(input_path, quality=85, max_width=1200):
    """压缩图片"""
    if not os.path.exists(input_path):
        print(f"❌ 文件不存在: {input_path}")
        return

    try:
        # 获取原始大小
        original_size = os.path.getsize(input_path) / 1024

        # 打开图片
        img = Image.open(input_path)

        # 如果图片宽度超过1200px，缩小
        if img.width > max_width:
            ratio = max_width / img.width
            new_height = int(img.height * ratio)
            img = img.resize((max_width, new_height), Image.Resampling.LANCZOS)

        # 保存压缩后的图片
        img.save(input_path, 'PNG', optimize=True, quality=quality)

        # 获取压缩后大小
        new_size = os.path.getsize(input_path) / 1024
        reduction = original_size - new_size
        reduction_percent = (reduction / original_size * 100) if original_size > 0 else 0

        print(f"✅ {os.path.basename(input_path)}")
        print(f"   原始: {original_size:.1f}KB → 压缩后: {new_size:.1f}KB (节省 {reduction:.1f}KB, {reduction_percent:.1f}%)")

    except Exception as e:
        print(f"❌ 压缩失败 {input_path}: {str(e)}")

print("开始压缩图片...\n")
for image_path in images_to_compress:
    compress_image(image_path)

print("\n✨ 压缩完成！")
