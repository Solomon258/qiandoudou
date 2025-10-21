#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
将风景图放入气泡框架中 - 通用版本
使用方法:
    python create_bubble_image.py <风景图路径> [输出文件名]

示例:
    python create_bubble_image.py "travel\新疆_compressed.jpg" "乌鲁木齐.jpg"
    python create_bubble_image.py "travel\西安.jpg"
"""

from PIL import Image, ImageDraw
import os
import sys

def create_circular_mask(size):
    """创建圆形蒙版"""
    mask = Image.new('L', size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size[0], size[1]), fill=255)
    return mask

def create_bubble_image(bubble_path, landscape_path, output_path):
    """
    将风景图放入气泡中

    参数:
        bubble_path: 气泡图片路径
        landscape_path: 风景图片路径
        output_path: 输出图片路径
    """
    # 读取气泡图片
    bubble = Image.open(bubble_path).convert('RGBA')

    # 读取风景图片
    landscape = Image.open(landscape_path).convert('RGBA')

    # 获取气泡尺寸
    bubble_width, bubble_height = bubble.size

    # 调整风景图大小以适配气泡
    # 使用稍小的尺寸以留出边框空间(约90%的气泡尺寸)
    target_size = int(bubble_width * 0.85)

    # 计算裁剪区域,保持风景图的中心部分
    landscape_width, landscape_height = landscape.size

    # 等比例缩放风景图,使其能够覆盖目标圆形区域
    scale = max(target_size / landscape_width, target_size / landscape_height)
    new_width = int(landscape_width * scale)
    new_height = int(landscape_height * scale)
    landscape_resized = landscape.resize((new_width, new_height), Image.Resampling.LANCZOS)

    # 从中心裁剪正方形区域
    left = (new_width - target_size) // 2
    top = (new_height - target_size) // 2
    right = left + target_size
    bottom = top + target_size
    landscape_cropped = landscape_resized.crop((left, top, right, bottom))

    # 创建圆形蒙版
    mask = create_circular_mask((target_size, target_size))

    # 创建最终图像
    result = Image.new('RGBA', (bubble_width, bubble_height), (255, 255, 255, 0))

    # 计算风景图在气泡中的位置(居中)
    x_offset = (bubble_width - target_size) // 2
    y_offset = (bubble_height - target_size) // 2

    # 将圆形风景图粘贴到结果图像中
    result.paste(landscape_cropped, (x_offset, y_offset), mask)

    # 将气泡框架叠加在上面
    result = Image.alpha_composite(result, bubble)

    # 转换为RGB并保存为JPG
    final_image = Image.new('RGB', result.size, (255, 255, 255))
    final_image.paste(result, mask=result.split()[3])

    # 保存
    final_image.save(output_path, 'JPEG', quality=95)
    print(f"✓ 图片已成功生成: {output_path}")
    print(f"  输出尺寸: {final_image.size}")
    return True

def print_usage():
    """打印使用说明"""
    print("=" * 60)
    print("气泡图片生成器 - 使用说明")
    print("=" * 60)
    print("\n用法:")
    print("  python create_bubble_image.py <风景图路径> [输出文件名]\n")
    print("参数:")
    print("  <风景图路径>  - 必需,风景图片的路径(相对或绝对路径)")
    print("  [输出文件名]  - 可选,输出的文件名(默认与输入文件同名)\n")
    print("示例:")
    print('  python create_bubble_image.py "travel\\新疆_compressed.jpg" "乌鲁木齐.jpg"')
    print('  python create_bubble_image.py "travel\\西安.jpg"')
    print('  python create_bubble_image.py "D:\\images\\landscape.jpg" "我的气泡图.jpg"')
    print("=" * 60)

if __name__ == '__main__':
    # 默认配置
    base_dir = r'D:\4_code\qiandoudou\qiandoudou-frontend\static\icon\paopao'
    bubble_path = os.path.join(base_dir, r'pao\泡泡@3x.png')
    output_dir = os.path.join(base_dir, 'travel')

    # 检查命令行参数
    if len(sys.argv) < 2:
        print("\n❌ 错误: 缺少必需的参数!\n")
        print_usage()
        sys.exit(1)

    # 获取风景图路径
    landscape_input = sys.argv[1]

    # 如果是相对路径,则相对于base_dir
    if not os.path.isabs(landscape_input):
        landscape_path = os.path.join(base_dir, landscape_input)
    else:
        landscape_path = landscape_input

    # 获取输出文件名
    if len(sys.argv) >= 3:
        output_filename = sys.argv[2]
    else:
        # 使用输入文件的文件名
        output_filename = os.path.basename(landscape_path)
        # 确保扩展名是.jpg
        if not output_filename.lower().endswith('.jpg'):
            name_without_ext = os.path.splitext(output_filename)[0]
            output_filename = name_without_ext + '.jpg'

    # 构建输出路径
    output_path = os.path.join(output_dir, output_filename)

    # 检查输入文件是否存在
    if not os.path.exists(bubble_path):
        print(f"\n❌ 错误: 气泡模板不存在: {bubble_path}")
        print("请确保气泡模板文件在正确的位置!")
        sys.exit(1)

    if not os.path.exists(landscape_path):
        print(f"\n❌ 错误: 风景图片不存在: {landscape_path}")
        print("请检查文件路径是否正确!")
        sys.exit(1)

    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)

    # 打印处理信息
    print("\n" + "=" * 60)
    print("开始生成气泡图片...")
    print("=" * 60)
    print(f"输入图片: {landscape_path}")
    print(f"气泡模板: {bubble_path}")
    print(f"输出文件: {output_path}")
    print("-" * 60)

    # 创建图片
    try:
        create_bubble_image(bubble_path, landscape_path, output_path)
        print("=" * 60)
        print("✓ 完成!")
        print("=" * 60 + "\n")
    except Exception as e:
        print(f"\n❌ 生成失败: {str(e)}")
        sys.exit(1)
