@echo off
chcp 65001 >nul
title 气泡图片生成器

:menu
cls
echo ============================================================
echo              气泡图片生成器
echo ============================================================
echo.
echo 请输入风景图片的路径:
echo 提示: 可以直接拖拽图片文件到此窗口
echo       或输入相对路径,如: travel\西安.jpg
echo.
set /p landscape_path="图片路径: "

:: 移除引号
set landscape_path=%landscape_path:"=%

if "%landscape_path%"=="" (
    echo.
    echo [错误] 未输入路径!
    pause
    goto menu
)

echo.
echo 请输入输出文件名 (可选,直接回车使用默认名称):
set /p output_name="输出文件名: "

echo.
echo ============================================================
echo 正在处理...
echo ============================================================

if "%output_name%"=="" (
    python create_bubble_image.py "%landscape_path%"
) else (
    python create_bubble_image.py "%landscape_path%" "%output_name%"
)

if %errorlevel% neq 0 (
    echo.
    echo ============================================================
    echo [提示] 如果提示找不到Pillow库,请先安装:
    echo        pip install Pillow
    echo ============================================================
)

echo.
echo.
echo 是否继续处理其他图片? (Y/N)
set /p continue="请选择: "

if /i "%continue%"=="Y" goto menu
if /i "%continue%"=="y" goto menu

exit
