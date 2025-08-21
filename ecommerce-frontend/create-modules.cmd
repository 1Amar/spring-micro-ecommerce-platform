@echo off
echo Creating placeholder feature modules...

mkdir src\app\features\products 2>nul
mkdir src\app\features\cart 2>nul
mkdir src\app\features\checkout 2>nul
mkdir src\app\features\profile 2>nul
mkdir src\app\features\orders 2>nul
mkdir src\app\features\search 2>nul
mkdir src\app\features\category 2>nul

echo Feature module directories created.
echo.
echo To generate full modules, run:
echo ng generate module features/products --routing
echo ng generate module features/cart --routing
echo ng generate module features/checkout --routing
echo ng generate module features/profile --routing
echo ng generate module features/orders --routing
echo ng generate module features/search --routing
echo ng generate module features/category --routing
echo.
pause