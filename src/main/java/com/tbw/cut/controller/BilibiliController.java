package com.tbw.cut.controller;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.service.BilibiliLiveService;
import com.tbw.cut.bilibili.service.BilibiliVideoService;
import com.tbw.cut.bilibili.service.BilibiliLoginService;
import com.tbw.cut.dto.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

/**
 * Bilibili API测试控制器
 * 提供用于测试Bilibili服务接口的REST API端点
 */
@Slf4j
@RestController
@RequestMapping("/api/bilibili")
public class BilibiliController {
    
    @Autowired
    @Qualifier("bilibiliLiveServiceImpl")
    private BilibiliLiveService bilibiliLiveService;
    
    @Autowired
    @Qualifier("bilibiliVideoServiceImpl")
    private BilibiliVideoService bilibiliVideoService;
    
    @Autowired
    @Qualifier("bilibiliLoginServiceImpl")
    private BilibiliLoginService bilibiliLoginService;
    
    // ==================== 直播相关接口 ====================
    
    /**
     * 获取主播直播状态
     * @param roomId 房间ID
     * @return 直播状态信息
     */
    @GetMapping("/live/status")
    public ResponseResult<JSONObject> getLiveStatus(@RequestParam String roomId) {
        try {
            JSONObject data = bilibiliLiveService.getLiveStatus(roomId);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取主播直播状态失败: roomId={}", roomId, e);
            return ResponseResult.error("获取主播直播状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取直播间基本信息
     * @param roomId 直播间号
     * @return 直播间基本信息
     */
    @GetMapping("/live/room/info")
    public ResponseResult<JSONObject> getRoomInfo(@RequestParam String roomId) {
        try {
            JSONObject data = bilibiliLiveService.getRoomInfo(roomId);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取直播间基本信息失败: roomId={}", roomId, e);
            return ResponseResult.error("获取直播间基本信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户对应的直播间状态
     * @param userId 用户ID
     * @return 直播间状态信息
     */
    @GetMapping("/live/room/info/old")
    public ResponseResult<JSONObject> getRoomInfoOld(@RequestParam String userId) {
        try {
            JSONObject data = bilibiliLiveService.getRoomInfoOld(userId);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取用户对应的直播间状态失败: userId={}", userId, e);
            return ResponseResult.error("获取用户对应的直播间状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取房间页初始化信息
     * @param roomId 直播间号（短号）
     * @return 房间初始化信息
     */
    @GetMapping("/live/room/init")
    public ResponseResult<JSONObject> getRoomInitInfo(@RequestParam String roomId) {
        try {
            JSONObject data = bilibiliLiveService.getRoomInitInfo(roomId);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取房间页初始化信息失败: roomId={}", roomId, e);
            return ResponseResult.error("获取房间页初始化信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取直播间基本信息（新接口）
     * @param roomIds 直播间ID列表
     * @return 直播间基本信息
     */
    @GetMapping("/live/room/base/info")
    public ResponseResult<JSONObject> getRoomBaseInfo(@RequestParam String[] roomIds) {
        try {
            JSONObject data = bilibiliLiveService.getRoomBaseInfo(roomIds);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取直播间基本信息失败: roomIds={}", (Object) roomIds, e);
            return ResponseResult.error("获取直播间基本信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量获取直播间状态
     * @param userIds 用户ID列表
     * @return 直播间状态信息
     */
    @GetMapping("/live/status/batch")
    public ResponseResult<JSONObject> getLiveStatusBatch(@RequestParam String[] userIds) {
        try {
            JSONObject data = bilibiliLiveService.getLiveStatusBatch(userIds);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("批量获取直播间状态失败: userIds={}", (Object) userIds, e);
            return ResponseResult.error("批量获取直播间状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取主播信息
     * @param userId 主播用户ID
     * @return 主播信息
     */
    @GetMapping("/live/anchor/info")
    public ResponseResult<JSONObject> getAnchorInfo(@RequestParam String userId) {
        try {
            JSONObject data = bilibiliLiveService.getAnchorInfo(userId);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取主播信息失败: userId={}", userId, e);
            return ResponseResult.error("获取主播信息失败: " + e.getMessage());
        }
    }
    
    // ==================== 视频相关接口 ====================
    
    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/video/user/info")
    public ResponseResult<JSONObject> getUserInfo(@RequestParam String userId) {
        try {
            JSONObject data = bilibiliVideoService.getUserInfo(userId);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取用户信息失败: userId={}", userId, e);
            return ResponseResult.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取视频信息
     * @param bvid 视频BV号
     * @return 视频信息
     */
    @GetMapping("/video/info")
    public ResponseResult<JSONObject> getVideoInfo(@RequestParam String bvid) {
        try {
            JSONObject data = bilibiliVideoService.getVideoInfo(bvid);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取视频信息失败: bvid={}", bvid, e);
            return ResponseResult.error("获取视频信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取视频播放信息
     * @param bvid 视频BV号
     * @param cid 视频CID
     * @return 播放信息
     */
    @GetMapping("/video/play/info")
    public ResponseResult<JSONObject> getVideoPlayInfo(@RequestParam String bvid, @RequestParam String cid) {
        try {
            JSONObject data = bilibiliVideoService.getVideoPlayInfo(bvid, cid);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取视频播放信息失败: bvid={}, cid={}", bvid, cid, e);
            return ResponseResult.error("获取视频播放信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 搜索视频
     * @param keyword 搜索关键词
     * @param page 页码
     * @param pageSize 每页数量
     * @return 搜索结果
     */
    @GetMapping("/video/search")
    public ResponseResult<JSONObject> searchVideos(@RequestParam String keyword, 
                                  @RequestParam(defaultValue = "1") int page, 
                                  @RequestParam(defaultValue = "20") int pageSize) {
        try {
            JSONObject data = bilibiliVideoService.searchVideos(keyword, page, pageSize);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("搜索视频失败: keyword={}, page={}, pageSize={}", keyword, page, pageSize, e);
            return ResponseResult.error("搜索视频失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取视频评论
     * @param oid 视频ID
     * @param type 评论类型
     * @param page 页码
     * @param pageSize 每页数量
     * @return 评论信息
     */
    @GetMapping("/video/comments")
    public ResponseResult<JSONObject> getVideoComments(@RequestParam String oid, 
                                      @RequestParam(defaultValue = "1") int type, 
                                      @RequestParam(defaultValue = "1") int page, 
                                      @RequestParam(defaultValue = "20") int pageSize) {
        try {
            JSONObject data = bilibiliVideoService.getVideoComments(oid, type, page, pageSize);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取视频评论失败: oid={}, type={}, page={}, pageSize={}", oid, type, page, pageSize, e);
            return ResponseResult.error("获取视频评论失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取视频详细信息(web端)
     * @param aid 稿件avid (可选)
     * @param bvid 稿件bvid (可选)
     * @return 视频详细信息
     */
    @GetMapping("/video/detail")
    public ResponseResult<JSONObject> getVideoDetail(@RequestParam(required = false) Long aid, 
                                    @RequestParam(required = false) String bvid) {
        try {
            JSONObject data = bilibiliVideoService.getVideoDetail(aid, bvid);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取视频详细信息失败: aid={}, bvid={}", aid, bvid, e);
            return ResponseResult.error("获取视频详细信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取视频超详细信息(web端)
     * @param aid 稿件avid (可选)
     * @param bvid 稿件bvid (可选)
     * @param needElec 是否获取UP主充电信息 (0:否, 1:是)
     * @return 视频超详细信息
     */
    @GetMapping("/video/detail/info")
    public ResponseResult<JSONObject> getVideoDetailInfo(@RequestParam(required = false) Long aid, 
                                        @RequestParam(required = false) String bvid, 
                                        @RequestParam(required = false, defaultValue = "0") Integer needElec) {
        try {
            JSONObject data = bilibiliVideoService.getVideoDetailInfo(aid, bvid, needElec);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取视频超详细信息失败: aid={}, bvid={}, needElec={}", aid, bvid, needElec, e);
            return ResponseResult.error("获取视频超详细信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取视频简介
     * @param aid 稿件avid (可选)
     * @param bvid 稿件bvid (可选)
     * @return 视频简介
     */
    @GetMapping("/video/description")
    public ResponseResult<String> getVideoDescription(@RequestParam(required = false) Long aid, 
                                     @RequestParam(required = false) String bvid) {
        try {
            String data = bilibiliVideoService.getVideoDescription(aid, bvid);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取视频简介失败: aid={}, bvid={}", aid, bvid, e);
            return ResponseResult.error("获取视频简介失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询视频分P列表 (avid/bvid转cid)
     * @param aid 稿件avid (可选)
     * @param bvid 稿件bvid (可选)
     * @return 视频分P列表
     */
    @GetMapping("/video/page/list")
    public ResponseResult<JSONObject> getVideoPageList(@RequestParam(required = false) Long aid, 
                                      @RequestParam(required = false) String bvid) {
        try {
            JSONObject data = bilibiliVideoService.getVideoPageList(aid, bvid);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("查询视频分P列表失败: aid={}, bvid={}", aid, bvid, e);
            return ResponseResult.error("查询视频分P列表失败: " + e.getMessage());
        }
    }
    
    // ==================== 登录相关接口 ====================
    
    /**
     * 获取Web端登录公钥和盐值
     * @return 公钥和盐值信息
     */
    @GetMapping("/login/key")
    public ResponseResult<JSONObject> getWebLoginKey() {
        try {
            JSONObject data = bilibiliLoginService.getWebLoginKey();
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取Web端登录公钥和盐值失败", e);
            return ResponseResult.error("获取Web端登录公钥和盐值失败: " + e.getMessage());
        }
    }
    
    /**
     * Web端密码登录 (测试用，实际使用时需要更安全的实现)
     * @param username 用户名
     * @param password 加密后的带盐密码
     * @param token 登录token
     * @param challenge 极验challenge
     * @param validate 极验result
     * @param seccode 极验result +|jordan
     * @param keep 是否保持登录
     * @param source 登录来源
     * @param goUrl 跳转URL
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseResult<JSONObject> webLogin(@RequestParam String username, 
                              @RequestParam String password, 
                              @RequestParam String token, 
                              @RequestParam String challenge, 
                              @RequestParam String validate, 
                              @RequestParam String seccode, 
                              @RequestParam(defaultValue = "0") int keep, 
                              @RequestParam(required = false) String source, 
                              @RequestParam(required = false) String goUrl) {
        try {
            JSONObject data = bilibiliLoginService.webLogin(username, password, token, challenge, validate, seccode, keep, source, goUrl);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("Web端密码登录失败: username={}", username, e);
            return ResponseResult.error("Web端密码登录失败: " + e.getMessage());
        }
    }
}