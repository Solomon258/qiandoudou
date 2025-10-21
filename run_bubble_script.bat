@echo off
chcp 65001
echo 正在生成气泡图片...
python create_bubble_image.py
if %errorlevel% neq 0 (
    echo Python命令失败,尝试使用python3...
    python3 create_bubble_image.py
)
if %errorlevel% neq 0 (
    echo.
    echo 错误: 无法运行Python脚本
    echo 请确保已安装Python和Pillow库
    echo 如果没有安装Pillow,请运行: pip install Pillow
    echo.
)
pause
