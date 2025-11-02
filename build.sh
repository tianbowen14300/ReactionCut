#!/bin/bash

# Bilibili视频处理系统构建脚本

echo "开始构建Bilibili视频处理系统..."

# 构建后端项目
echo "1. 构建后端项目..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "后端项目构建失败!"
    exit 1
fi

echo "后端项目构建完成!"

# 构建前端项目
echo "2. 构建前端项目..."
cd frontend
npm install

if [ $? -ne 0 ]; then
    echo "前端依赖安装失败!"
    exit 1
fi

npm run build

if [ $? -ne 0 ]; then
    echo "前端项目构建失败!"
    exit 1
fi

echo "前端项目构建完成!"

echo "项目构建完成!"
echo "后端启动: java -jar target/reaction-cut-0.0.1-SNAPSHOT.jar"
echo "前端启动: cd frontend && npm run dev"