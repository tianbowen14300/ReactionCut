# Bilibili Video Processing System

这是一个基于Spring Boot的Bilibili视频处理系统，主要功能包括：

1. Bilibili主播订阅与实时录播
2. Bilibili视频下载
3. 本地视频截取、拼接与自动投稿

## 技术栈

### 后端
- Java 8
- Spring Boot 2.7.13
- MySQL
- MyBatis Plus
- FFmpeg

### 前端
- Vue 2
- Element UI

## 功能模块

### 1. Bilibili主播订阅与实时录播
- 订阅指定Bilibili主播
- 定时检查主播直播状态
- 检测到开播后自动录制并存储视频文件

### 2. Bilibili视频下载
- 输入Bilibili视频链接进行视频下载
- 异步处理下载任务
- 实时反馈下载进度

### 3. 本地视频截取、拼接与自动投稿
- 选择本地视频文件
- 设置每个视频的截取开始时间和结束时间
- 按顺序截取视频片段并拼接为完整视频
- 调用Bilibili视频上传接口进行投稿

## 项目结构

```
.
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/tbw/cut/
│   │   │       ├── bilibili/        # Bilibili API调用模块
│   │   │       │   ├── config/      # Bilibili配置类
│   │   │       │   ├── constant/    # Bilibili API常量
│   │   │       │   ├── impl/        # 旧版Bilibili服务实现（已废弃）
│   │   │       │   ├── service/     # Bilibili服务接口
│   │   │       │   │   └── impl/    # Bilibili服务实现
│   │   │       │   └── *.java       # Bilibili工具类和接口
│   │   │       ├── config/          # 配置类
│   │   │       ├── controller/      # 控制器
│   │   │       ├── dto/             # 数据传输对象
│   │   │       ├── entity/          # 实体类
│   │   │       ├── exception/       # 异常处理
│   │   │       ├── mapper/          # MyBatis Mapper接口
│   │   │       ├── service/         # 业务逻辑接口
│   │   │       │   └── impl/        # 业务逻辑实现
│   │   │       └── utils/           # 工具类 (FFmpeg集成)
│   │   └── resources/
│   │       ├── db/
│   │       │   └── migration/       # 数据库迁移脚本
│   │       └── application.yml      # 配置文件
│   └── test/
└── frontend/                        # 前端项目
    ├── src/
    │   ├── api/                     # API接口封装
    │   ├── components/              # 公共组件
    │   ├── views/                   # 页面组件
    │   ├── utils/                   # 工具类
    │   ├── router.js                # 路由配置
    │   └── main.js                  # 入口文件
    ├── package.json                 # 项目依赖配置
    └── vue.config.js                # 项目配置文件
```

## Bilibili API调用模块

系统包含一个专门用于Bilibili API调用的模块，提供以下功能：

### 核心组件
1. **BilibiliApiClient**: 通用的HTTP客户端，用于发送请求到Bilibili API
2. **BilibiliApiResponse**: Bilibili API响应的封装类
3. **BilibiliApiResponseParser**: Bilibili API响应解析器
4. **BilibiliApiConstants**: Bilibili API路径常量类，用于统一管理所有API端点路径
5. **BilibiliService**: Bilibili服务接口（已废弃，请使用专门的服务接口）
6. **BilibiliServiceImpl**: Bilibili服务实现（已废弃，请使用专门的服务实现）
7. **BilibiliUtils**: Bilibili相关的工具类
8. **BilibiliLoginUtils**: Bilibili登录相关工具类

### 专门服务接口（推荐使用）
1. **BilibiliLiveService**: 直播相关API接口
2. **BilibiliVideoService**: 视频相关API接口
3. **BilibiliLoginService**: 登录相关API接口
4. **BilibiliQRCodeLoginService**: 二维码登录服务接口
5. **BilibiliUnifiedService**: 统一服务接口（继承所有专门服务接口）

### 主要功能分类

#### 直播相关API接口
1. **getLiveStatus(String roomId)**: 获取主播直播状态
2. **getRoomInfo(String roomId)**: 获取直播间基本信息
3. **getRoomInfoOld(String userId)**: 获取用户对应的直播间状态
4. **getRoomInitInfo(String roomId)**: 获取房间页初始化信息
5. **getRoomBaseInfo(String... roomIds)**: 获取直播间基本信息（新接口）
6. **getLiveStatusBatch(String... userIds)**: 批量获取直播间状态
7. **getAnchorInfo(String userId)**: 获取主播信息

#### 视频相关API接口
1. **getUserInfo(String userId)**: 获取用户信息
2. **getVideoInfo(String bvid)**: 获取视频信息
3. **getVideoPlayInfo(String bvid, String cid)**: 获取视频播放信息
4. **searchVideos(String keyword, int page, int pageSize)**: 搜索视频
5. **getVideoComments(String oid, int type, int page, int pageSize)**: 获取视频评论
6. **getVideoDetail(Long aid, String bvid)**: 获取视频详细信息(web端)
7. **getVideoDetailInfo(Long aid, String bvid, Integer needElec)**: 获取视频超详细信息(web端)
8. **getVideoDescription(Long aid, String bvid)**: 获取视频简介
9. **getVideoPageList(Long aid, String bvid)**: 查询视频分P列表 (avid/bvid转cid)

#### 登录相关API接口
1. **getWebLoginKey()**: 获取Web端登录公钥和盐值
2. **webLogin(...)**: Web端密码登录
3. **generateQRCode()**: 申请二维码(web端)
4. **pollQRCodeLogin(String qrcodeKey)**: 扫码登录(web端)

#### 二维码登录服务接口
1. **generateQRCode()**: 生成二维码
2. **pollQRCodeStatus(String qrcodeKey)**: 轮询扫码登录状态

### 测试API端点

为了方便测试Bilibili API功能，系统提供了REST API端点：

#### 直播相关端点
- `GET /api/bilibili/live/status?roomId={roomId}`: 获取主播直播状态
- `GET /api/bilibili/live/room/info?roomId={roomId}`: 获取直播间基本信息
- `GET /api/bilibili/live/room/info/old?userId={userId}`: 获取用户对应的直播间状态
- `GET /api/bilibili/live/room/init?roomId={roomId}`: 获取房间页初始化信息
- `GET /api/bilibili/live/room/base/info?roomIds={roomIds}`: 获取直播间基本信息（新接口）
- `GET /api/bilibili/live/status/batch?userIds={userIds}`: 批量获取直播间状态
- `GET /api/bilibili/live/anchor/info?userId={userId}`: 获取主播信息

#### 视频相关端点
- `GET /api/bilibili/video/user/info?userId={userId}`: 获取用户信息
- `GET /api/bilibili/video/info?bvid={bvid}`: 获取视频信息
- `GET /api/bilibili/video/play/info?bvid={bvid}&cid={cid}`: 获取视频播放信息
- `GET /api/bilibili/video/search?keyword={keyword}&page={page}&pageSize={pageSize}`: 搜索视频
- `GET /api/bilibili/video/comments?oid={oid}&type={type}&page={page}&pageSize={pageSize}`: 获取视频评论
- `GET /api/bilibili/video/detail?aid={aid}&bvid={bvid}`: 获取视频详细信息
- `GET /api/bilibili/video/detail/info?aid={aid}&bvid={bvid}&needElec={needElec}`: 获取视频超详细信息
- `GET /api/bilibili/video/description?aid={aid}&bvid={bvid}`: 获取视频简介
- `GET /api/bilibili/video/page/list?aid={aid}&bvid={bvid}`: 查询视频分P列表

#### 登录相关端点
- `GET /api/bilibili/login/key`: 获取Web端登录公钥和盐值
- `POST /api/bilibili/login`: Web端密码登录

#### 二维码登录端点
- `GET /api/bilibili/auth/qrcode/generate`: 生成二维码
- `GET /api/bilibili/auth/qrcode/poll?qrcodeKey={qrcodeKey}`: 轮询扫码登录状态
- `GET /api/bilibili/auth/status`: 检查登录状态
- `POST /api/bilibili/auth/logout`: 注销登录

### 二维码登录流程

系统实现了完整的Bilibili二维码登录功能，流程如下：

1. **生成二维码**: 调用`generateQRCode()`方法获取二维码URL和秘钥
2. **显示二维码**: 在前端显示二维码图片供手机扫描
3. **轮询状态**: 使用二维码秘钥轮询扫码登录状态
4. **状态处理**:
   - 未扫描: 继续轮询
   - 已扫描未确认: 提示用户在手机上确认
   - 登录成功: 获取并存储认证信息
   - 二维码失效: 提示用户重新操作

### 认证信息管理

系统会自动管理Bilibili的认证信息：

1. **登录成功后**: 认证信息会保存在`bilibili_login_info.json`文件中
2. **调用API时**: 自动从文件中读取认证信息并添加到请求头
3. **注销登录**: 清除认证信息文件

### 前端用户界面

前端界面在右上角提供用户头像：

1. **未登录状态**: 显示默认头像，点击后显示登录选项
2. **登录状态**: 显示用户头像，点击后显示注销选项
3. **扫码登录**: 点击登录后弹出二维码对话框

### 密码加密工具
**BilibiliLoginUtils** 提供了以下功能：
- **encryptPassword()**: 使用RSA公钥和盐值加密密码
- **getEncryptedPassword()**: 获取用于Bilibili登录的加密密码

### 使用示例
```java
// 推荐使用专门的服务接口
@Autowired
private BilibiliLiveService bilibiliLiveService;

@Autowired
private BilibiliVideoService bilibiliVideoService;

@Autowired
private BilibiliLoginService bilibiliLoginService;

@Autowired
private BilibiliQRCodeLoginService bilibiliQRCodeLoginService;

// 获取主播直播状态
JSONObject liveStatus = bilibiliLiveService.getLiveStatus("123456");

// 获取视频信息
JSONObject videoInfo = bilibiliVideoService.getVideoInfo("BV1Xx411175n");

// 获取Web端登录公钥和盐值
JSONObject loginKey = bilibiliLoginService.getWebLoginKey();

// 密码加密
String encryptedPassword = BilibiliLoginUtils.getEncryptedPassword("myPassword", loginKey);

// Web端登录
JSONObject loginResult = bilibiliLoginService.webLogin(
    "username", 
    encryptedPassword, 
    "token", 
    "challenge", 
    "validate", 
    "seccode", 
    0, 
    "main_web", 
    "https://www.bilibili.com"
);

// 生成二维码
JSONObject qrCode = bilibiliQRCodeLoginService.generateQRCode();

// 轮询二维码状态
BilibiliQRCodeLoginService.PollResult result = bilibiliQRCodeLoginService.pollQRCodeStatus("qrcode_key");

// 获取视频详细信息
JSONObject videoDetail = bilibiliVideoService.getVideoDetail(null, "BV117411r7R1");
JSONObject videoDetail2 = bilibiliVideoService.getVideoDetail(85440373L, null);

// 获取视频超详细信息
JSONObject videoDetailInfo = bilibiliVideoService.getVideoDetailInfo(170001L, null, 1);

// 获取视频简介
String description = bilibiliVideoService.getVideoDescription(39330059L, null);

// 查询视频分P列表
JSONObject pageList = bilibiliVideoService.getVideoPageList(13502509L, null);
```

## FFmpeg 集成

系统通过统一的FFmpeg工具类([FFmpegUtil](src/main/java/com/tbw/cut/utils/FFmpegUtil.java))处理所有视频操作：

1. **直播录制**: 实时录制Bilibili主播直播流
2. **视频下载**: 下载Bilibili视频内容
3. **视频剪辑**: 精确截取视频片段
4. **视频合并**: 无缝拼接多个视频片段
5. **视频信息**: 获取视频元数据信息

所有FFmpeg操作都通过shell命令执行，确保高性能和稳定性。

## 数据库设计

系统使用MySQL数据库，包含以下表：

1. `anchor` - 主播信息表
2. `video_download` - 视频下载记录表
3. `video_process_task` - 视频处理任务表
4. `video_clip` - 视频片段表

## API接口

### 主播相关接口
- `POST /api/anchor/subscribe` - 批量订阅主播
- `GET /api/anchor/list` - 获取所有订阅的主播
- `POST /api/anchor/check` - 手动检查主播直播状态

### 视频下载接口
- `POST /api/video/download` - 下载Bilibili视频
- `GET /api/video/download/{taskId}` - 查询下载任务状态

### 视频处理接口
- `POST /api/video/process` - 创建视频处理任务
- `GET /api/video/process/{taskId}` - 查询处理任务状态

## 配置说明

在`application.yml`中配置以下参数：

- 数据库连接信息
- FFmpeg路径
- 视频存储目录
- 临时文件目录
- Bilibili API相关配置

## 部署说明

### 后端部署
1. 安装MySQL数据库
2. 安装FFmpeg
3. 配置数据库连接
4. 构建项目：`mvn clean package`
5. 运行项目：`java -jar target/reaction-cut-0.0.1-SNAPSHOT.jar`

### 前端部署
1. 进入frontend目录：`cd frontend`
2. 安装依赖：`npm install`
3. 启动开发服务器：`npm run dev`
4. 构建生产版本：`npm run build`

### 开发环境
- 后端地址：http://localhost:8080
- 前端地址：http://localhost:8081
- 前端通过代理访问后端API