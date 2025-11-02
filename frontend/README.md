# Bilibili Video Processing System Frontend

这是Bilibili视频处理系统的前端项目，基于Vue 2和Element UI构建。

## 功能模块

1. 主播订阅管理
2. Bilibili视频下载
3. 视频处理与投稿

## 技术栈

- Vue 2
- Element UI
- Axios

## 项目结构

```
src/
├── api/          # API接口封装
├── components/   # 公共组件
├── views/        # 页面组件
├── utils/        # 工具类
├── router.js     # 路由配置
└── main.js       # 入口文件
```

## 开发环境

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm run dev
```

### 构建生产版本
```bash
npm run build
```

## 代理配置

开发环境下，前端通过代理访问后端API：
- 前端地址：http://localhost:8081
- 后端地址：http://localhost:8080
- API代理路径：/api

## 页面说明

1. **主播订阅管理** (`/anchor`)
   - 订阅Bilibili主播
   - 查看已订阅主播列表
   - 手动检查主播直播状态

2. **视频下载** (`/download`)
   - 输入Bilibili视频链接进行下载
   - 实时查看下载进度

3. **视频处理与投稿** (`/process`)
   - 选择本地视频文件
   - 设置截取时间段
   - 提交处理任务
   - 查看处理进度和投稿结果