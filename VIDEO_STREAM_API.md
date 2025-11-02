# 视频流URL获取API说明

## 功能说明
本系统提供了获取Bilibili视频流URL的功能，支持多种清晰度和格式，包括DASH和MP4格式。

## 后端API接口

### 1. 获取视频播放信息（通过bvid）
```
GET /api/video/stream/playurl
```

**请求参数：**
- `bvid` (必需): 视频BV号
- `cid` (必需): 视频CID
- `qn` (可选): 视频清晰度标识
- `fnval` (可选): 视频流格式标识
- `fnver` (可选): 视频流版本标识
- `fourk` (可选): 是否允许4K视频

**响应示例：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    // 视频播放信息
  }
}
```

### 2. 获取视频播放信息（通过aid）
```
GET /api/video/stream/playurl/aid
```

**请求参数：**
- `aid` (必需): 视频AV号
- `cid` (必需): 视频CID
- `qn` (可选): 视频清晰度标识
- `fnval` (可选): 视频流格式标识
- `fnver` (可选): 视频流版本标识
- `fourk` (可选): 是否允许4K视频

## 前端API调用

### 1. 获取视频流URL（通过bvid）
```javascript
import { getVideoPlayUrl } from '@/api/video'

const response = await getVideoPlayUrl('BV1Xx411175n', '171776208', {
  qn: '112',
  fnval: '4048',
  fnver: '0',
  fourk: '1'
})
```

### 2. 获取视频流URL（通过aid）
```javascript
import { getVideoPlayUrlByAid } from '@/api/video'

const response = await getVideoPlayUrlByAid('99999999', '171776208', {
  qn: '112',
  fnval: '4048',
  fnver: '0',
  fourk: '1'
})
```

## 参数说明

### qn - 视频清晰度标识
| 值 | 含义 | 备注 |
|---|---|---|
| 6 | 240P 极速 | 仅MP4格式支持 |
| 16 | 360P 流畅 | |
| 32 | 480P 清晰 | |
| 64 | 720P 高清 | WEB端默认值 |
| 74 | 720P60 高帧率 | 需要登录认证 |
| 80 | 1080P 高清 | TV端与APP端默认值，需要登录认证 |
| 100 | 智能修复 | 需要大会员认证 |
| 112 | 1080P+ 高码率 | 需要大会员认证 |
| 116 | 1080P60 高帧率 | 需要大会员认证 |
| 120 | 4K 超清 | 需要fnval&128=128且fourk=1，需要大会员认证 |
| 125 | HDR 真彩色 | 仅支持DASH格式，需要fnval&64=64，需要大会员认证 |
| 126 | 杜比视界 | 仅支持DASH格式，需要fnval&512=512，需要大会员认证 |
| 127 | 8K 超高清 | 仅支持DASH格式，需要fnval&1024=1024，需要大会员认证 |

### fnval - 视频流格式标识
| 值 | 含义 | 备注 |
|---|---|---|
| 0 | FLV 格式 | FLV格式已下线 |
| 1 | MP4 格式 | 仅H.264编码 |
| 16 | DASH 格式 | |
| 64 | 是否需求HDR视频 | 需求DASH格式，需要qn=125 |
| 128 | 是否需求4K分辨率 | 需要qn=120 |
| 256 | 是否需求杜比音频 | 需求DASH格式 |
| 512 | 是否需求杜比视界 | 需求DASH格式 |
| 1024 | 是否需求8K分辨率 | 需求DASH格式，需要qn=127 |
| 2048 | 是否需求AV1编码 | 需求DASH格式 |
| 4048 | 所有可用DASH视频流 | 一次性返回所有可用DASH格式视频流 |

### fnver - 视频流版本标识
目前该值恒为0

### fourk - 是否允许4K视频
- 0: 画质最高1080P（默认）
- 1: 画质最高4K

## 使用示例

### 1. 获取1080P+ DASH格式视频流
```javascript
const response = await getVideoPlayUrl(bvid, cid, {
  qn: '112',     // 1080P+ 高码率
  fnval: '4048', // 所有DASH格式
  fnver: '0',    // 版本标识
  fourk: '1'     // 允许4K
})
```

### 2. 获取720P MP4格式视频流
```javascript
const response = await getVideoPlayUrl(bvid, cid, {
  qn: '64',   // 720P 高清
  fnval: '1', // MP4格式
  fnver: '0', // 版本标识
  fourk: '0'  // 不允许4K
})
```

## 返回数据结构

### DASH格式返回示例
```json
{
  "code": 0,
  "data": {
    "dash": {
      "video": [
        {
          "id": 112,
          "baseUrl": "https://example.com/video.m4s",
          "backupUrl": ["https://backup.example.com/video.m4s"],
          "bandwidth": 123456,
          "mimeType": "video/mp4",
          "codecs": "avc1.640032",
          "width": 1920,
          "height": 1080,
          "frameRate": "30",
          "codecid": 7
        }
      ],
      "audio": [
        {
          "id": 30280,
          "baseUrl": "https://example.com/audio.m4s",
          "backupUrl": ["https://backup.example.com/audio.m4s"],
          "bandwidth": 12345,
          "mimeType": "audio/mp4",
          "codecs": "mp4a.40.2"
        }
      ]
    }
  }
}
```

### MP4/FLV格式返回示例
```json
{
  "code": 0,
  "data": {
    "durl": [
      {
        "order": 1,
        "length": 123456,
        "size": 1234567,
        "url": "https://example.com/video.mp4",
        "backup_url": ["https://backup.example.com/video.mp4"]
      }
    ]
  }
}
```

## 注意事项

1. 获取高清晰度视频需要登录认证
2. 获取4K、HDR、杜比视界等高级格式需要大会员认证
3. DASH格式需要同时下载视频和音频流并进行合并
4. URL有效时间为120分钟，超时需要重新获取
5. 部分视频可能有分段，需要特别处理