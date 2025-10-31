#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
更新 dream_items 表中所有商品的 modal_image_url 字段
确保弹框显示的图片地址正确
"""

import mysql.connector
from mysql.connector import Error
import sys

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',  # 修改为你的数据库主机
    'user': 'root',       # 修改为你的数据库用户
    'password': '',       # 修改为你的数据库密码
    'database': 'qiandoudou'  # 数据库名
}

# 更新映射：商品名称 -> 弹框图片 URL
MODAL_IMAGE_MAP = {
    # 购物商品
    'car': '/static/icon/sel-car.png',
    'suv': '/static/icon/sel-suv.png',
    'supercar': '/static/icon/sel-supercar.png',
    'bag': '/static/icon/sel-bag.png',
    'rolex': '/static/icon/sel-rolex.png',
    # 旅行目的地
    'beijing': '/static/icon/sel-beijing.png',
    'xian': '/static/icon/sel-xian.png',
    'urumqi': '/static/icon/sel-urumqi.png',
    'sydney': '/static/icon/sel-sydney.png',
    'tokyo': '/static/icon/sel-tokyo.png',
}

def update_modal_images():
    """更新数据库中的 modal_image_url"""
    try:
        # 连接数据库
        connection = mysql.connector.connect(**DB_CONFIG)
        cursor = connection.cursor()

        print("=" * 60)
        print("开始更新 dream_items 表的 modal_image_url 字段")
        print("=" * 60)

        # 更新每个商品的 modal_image_url
        for item_name, modal_image_url in MODAL_IMAGE_MAP.items():
            update_sql = """
                UPDATE dream_items
                SET modal_image_url = %s
                WHERE name = %s AND deleted = 0
            """
            cursor.execute(update_sql, (modal_image_url, item_name))
            updated_rows = cursor.rowcount
            print(f"✓ 更新 '{item_name}': {updated_rows} 行 -> {modal_image_url}")

        # 提交事务
        connection.commit()
        print("\n" + "=" * 60)
        print("所有更新已提交")
        print("=" * 60)

        # 验证更新结果
        print("\n验证更新结果：")
        print("-" * 60)

        select_sql = """
            SELECT
                category,
                name,
                display_name,
                modal_image_url
            FROM dream_items
            WHERE deleted = 0
            ORDER BY category, sort_order
        """
        cursor.execute(select_sql)
        results = cursor.fetchall()

        for row in results:
            category, name, display_name, modal_image_url = row
            category_text = "购物" if category == 1 else "旅行"
            print(f"[{category_text}] {display_name:15} ({name:15}) -> {modal_image_url}")

        print("-" * 60)
        print(f"总计: {len(results)} 项")

        cursor.close()
        connection.close()
        print("\n✓ 数据库连接已关闭")
        print("\n提示：")
        print("1. 后端需要重启以加载新的数据")
        print("2. 前端需要清除缓存并刷新")
        return True

    except Error as e:
        print(f"✗ 数据库错误: {e}", file=sys.stderr)
        return False
    except Exception as e:
        print(f"✗ 其他错误: {e}", file=sys.stderr)
        return False


if __name__ == '__main__':
    print("\n数据库配置:")
    print(f"  主机: {DB_CONFIG['host']}")
    print(f"  用户: {DB_CONFIG['user']}")
    print(f"  数据库: {DB_CONFIG['database']}")
    print()

    success = update_modal_images()
    sys.exit(0 if success else 1)
