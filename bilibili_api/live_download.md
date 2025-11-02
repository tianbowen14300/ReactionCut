BAC DocumentBAC Document
首页目录贡献指南
相关社群
GitHub

直播间视频流

根据真实直播间号获取直播视频流
直播间视频流
根据真实直播间号获取直播视频流
https://api.live.bilibili.com/room/v1/Room/playUrl

请求方式：GET

url参数：

参数名	类型	内容	必要性	备注
cid	num	目标真实直播间号	必要	直播间的room_id（非短号）
platform	str	直播流格式	非必要	h5：hls方式
web：http-flv方式
默认为http-flv方式
quality	num	画质	非必要	qn与quality任选其一
2：流畅
3：高清
4：原画
qn	str	画质	非必要	qn与quality任选其一
80：流畅
150：高清
400：蓝光
10000：原画
20000：4K
30000：杜比
json回复：

根对象：

字段	类型	内容	备注
code	num	返回值	0：成功
-400：参数错误
19002003：房间信息不存在
message	str	错误信息	默认为0
ttl	str	1
data	obj	信息本体
data对象：

字段	类型	内容	备注
current_quality	num	当前画质代码qn
accept_quality	array	可选画质数参数
current_qn	num	当前画质代码quality
quality_description	array	可选画质参数quality
durl	array	直播流url组
accept_quality数组：

项	类型	内容	备注
0	str	qn画质代码1
n	str	qn画质代码(n+1)
……	str	……	……
quality_description数组：

项	类型	内容	备注
0	obj	画质代码1
n	obj	画质代码(n+1)
……	obj	……	……
quality_description数组中的对象：

字段	类型	内容	备注
qn	num	画质代码
desc	str	该代码对应的画质名称
durl数组：

项	类型	内容	备注
0	obj	主线服务器
n	obj	备线n服务器
……	obj	……	……
durl数组中的对象：

字段	类型	内容	备注
url	str	直播流url	flv或m3u8格式
注：带有转义
length	num	0	作用尚不明确
order	num	服务器线路序号
stream_type	num	0	作用尚不明确
p2p_type	num	0	作用尚不明确
示例：

查询直播间cid=14073662的直播间信息


curl -G 'https://api.live.bilibili.com/room/v1/Room/playUrl' \
--data-urlencode 'cid=14073662' \
--data-urlencode 'qn=10000' \
--data-urlencode 'platform=web'
查看响应示例：

{
"code": 0,
"message": "0",
"ttl": 1,
"data": {
"current_quality": 3,
"accept_quality": [
"4",
"3",
"2"
],
"current_qn": 3,
"quality_description": [
{
"qn": 4,
"desc": "原画"
},
{
"qn": 3,
"desc": "高清"
},
{
"qn": 2,
"desc": "流畅"
}
],
"durl": [
{
"url": "https://d1--cn-gotcha04.bilivideo.com/live-bvc/601131/live_14073662_bs_3699814_1500.flv?cdn=cn-gotcha04&expires=1602496530&len=0&oi=1939228219&pt=&qn=150&trid=e6540d81a5d04c2ea459c46ebe77472a&sigparams=cdn,expires,len,oi,pt,qn,trid&sign=20e4ac695fbdd1d11d5dac4f93caa783&ptype=0&src=9&sl=1&order=1",
"length": 0,
"order": 1,
"stream_type": 0,
"p2p_type": 0
},
{
"url": "https://d1--cn-gotcha01.bilivideo.com/live-bvc/757951/live_14073662_bs_3699814_1500.flv?cdn=cn-gotcha01&expires=1602496530&len=0&oi=1939228219&pt=&qn=150&trid=e6540d81a5d04c2ea459c46ebe77472a&sigparams=cdn,expires,len,oi,pt,qn,trid&sign=9deea67bb9e9c1f0fa3886a34aa09473&ptype=0&src=9&sl=1&order=2",
"length": 0,
"order": 2,
"stream_type": 0,
"p2p_type": 0
},
{
"url": "https://d1--cn-gotcha04.bilivideo.com/live-bvc/982058/live_14073662_bs_3699814_1500.flv?cdn=cn-gotcha04&expires=1602496530&len=0&oi=1939228219&pt=&qn=150&trid=e6540d81a5d04c2ea459c46ebe77472a&sigparams=cdn,expires,len,oi,pt,qn,trid&sign=8753b270960034660184d975d86c0161&ptype=0&src=9&sl=1&order=3",
"length": 0,
"order": 3,
"stream_type": 0,
"p2p_type": 0
},
{
"url": "https://d1--cn-gotcha04.bilivideo.com/live-bvc/139554/live_14073662_bs_3699814_1500.flv?cdn=cn-gotcha04&expires=1602496530&len=0&oi=1939228219&pt=&qn=150&trid=e6540d81a5d04c2ea459c46ebe77472a&sigparams=cdn,expires,len,oi,pt,qn,trid&sign=cf93474e923c9ba8288c45d954f81045&ptype=0&src=9&sl=1&order=4",
"length": 0,
"order": 4,
"stream_type": 0,
"p2p_type": 0
}
]
}
}
在 GitHub 上编辑此页
Copyright © 2020-2025 SocialSisterYi | CC-BY-NC-4.0 Licensed
