package com.tbw.cut.bilibili.service;

import com.alibaba.fastjson.JSONObject;

/**
 * Bilibili视频相关服务接口
 */
public interface BilibiliVideoService {
    
    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    JSONObject getUserInfo(String userId);
    
    /**
     * 获取视频信息
     * @param bvid 视频BV号
     * @return 视频信息
     */
    JSONObject getVideoInfo(String bvid);
    
    /**
     * 获取视频播放信息
     * @param bvid 视频BV号
     * @param cid 视频CID
     * @return 播放信息
     */
    JSONObject getVideoPlayInfo(String bvid, String cid);
    
    /**
     * 获取视频播放信息（支持WBI签名和自定义参数）
     * @param bvid 视频BV号
     * @param cid 视频CID
     * @param qn 视频清晰度标识
     * @param fnval 视频流格式标识
     * @param fnver 视频流版本标识
     * @param fourk 是否允许4K视频
     * @return 播放信息
     */
    JSONObject getVideoPlayInfo(String bvid, String cid, String qn, String fnval, String fnver, String fourk);
    
    /**
     * 获取视频播放信息（支持avid和自定义参数）
     * @param aid 视频AV号
     * @param cid 视频CID
     * @param qn 视频清晰度标识
     * @param fnval 视频流格式标识
     * @param fnver 视频流版本标识
     * @param fourk 是否允许4K视频
     * @return 播放信息
     */
    JSONObject getVideoPlayInfoByAid(String aid, String cid, String qn, String fnval, String fnver, String fourk);
    
    /**
     * 搜索视频
     * @param keyword 搜索关键词
     * @param page 页码
     * @param pageSize 每页数量
     * @return 搜索结果
     */
    JSONObject searchVideos(String keyword, int page, int pageSize);
    
    /**
     * 获取视频评论
     * @param oid 视频ID
     * @param type 评论类型
     * @param page 页码
     * @param pageSize 每页数量
     * @return 评论信息
     */
    JSONObject getVideoComments(String oid, int type, int page, int pageSize);
    
    /**
     * 获取视频详细信息(web端)
     * @param aid 稿件avid (可选)
     * @param bvid 稿件bvid (可选)
     * @return 视频详细信息
     */
    JSONObject getVideoDetail(Long aid, String bvid);
    
    /**
     * 获取视频超详细信息(web端)
     * @param aid 稿件avid (可选)
     * @param bvid 稿件bvid (可选)
     * @param needElec 是否获取UP主充电信息 (0:否, 1:是)
     * @return 视频超详细信息
     */
    JSONObject getVideoDetailInfo(Long aid, String bvid, Integer needElec);
    
    /**
     * 获取视频简介
     * @param aid 稿件avid (可选)
     * @param bvid 稿件bvid (可选)
     * @return 视频简介
     */
    String getVideoDescription(Long aid, String bvid);
    
    /**
     * 查询视频分P列表 (avid/bvid转cid)
     * @param aid 稿件avid (可选)
     * @param bvid 稿件bvid (可选)
     * @return 视频分P列表
     */
    JSONObject getVideoPageList(Long aid, String bvid);
}