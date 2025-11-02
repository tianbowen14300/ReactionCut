package com.tbw.cut.bilibili.constant;

/**
 * Bilibili API接口路径常量类
 * 用于存储所有Bilibili API的端点路径，实现解耦和统一管理
 */
public class BilibiliApiConstants {
    
    // 私有构造函数防止实例化
    private BilibiliApiConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
    
    // ==================== 用户相关API ====================
    /** 获取用户信息 */
    public static final String USER_INFO = "/x/space/acc/info";
    
    // ==================== 视频相关API ====================
    /** 获取视频信息 */
    public static final String VIDEO_VIEW = "/x/web-interface/view";
    
    /** 获取视频播放信息 */
    public static final String VIDEO_PLAY_URL = "/x/player/playurl";
    
    /** 搜索视频 */
    public static final String VIDEO_SEARCH = "/x/web-interface/search/type";
    
    /** 获取视频评论 */
    public static final String VIDEO_COMMENTS = "/x/v2/reply";
    
    /** 获取视频详细信息 */
    public static final String VIDEO_DETAIL = "/x/web-interface/view/detail";
    
    /** 获取视频简介 */
    public static final String VIDEO_DESCRIPTION = "/x/web-interface/archive/desc";
    
    /** 获取视频分P列表 */
    public static final String VIDEO_PAGE_LIST = "/x/player/pagelist";
    
    // ==================== 直播相关API ====================
    /** 获取主播直播状态 */
    public static final String LIVE_ROOM_INFO = "/x/live/web-room/v1/index/getInfoByRoom";
    
    /** 获取直播间基本信息 */
    public static final String LIVE_ROOM_GET_INFO = "/room/v1/Room/get_info";
    
    /** 获取用户对应的直播间状态 */
    public static final String LIVE_ROOM_GET_INFO_OLD = "/room/v1/Room/getRoomInfoOld";
    
    /** 获取房间页初始化信息 */
    public static final String LIVE_ROOM_INIT = "/room/v1/Room/room_init";
    
    /** 获取直播间基本信息（新接口） */
    public static final String LIVE_ROOM_BASE_INFO = "/xlive/web-room/v1/index/getRoomBaseInfo";
    
    /** 批量获取直播间状态 */
    public static final String LIVE_ROOM_STATUS_BATCH = "/room/v1/Room/get_status_info_by_uids";
    
    // ==================== 登录相关API ====================
    /** 获取Web端登录公钥和盐值 */
    public static final String LOGIN_WEB_KEY = "/x/passport-login/web/key";
    
    /** Web端密码登录 */
    public static final String LOGIN_WEB = "/x/passport-login/web/login";
    
    /** 申请二维码(web端) */
    public static final String LOGIN_QR_CODE_GENERATE = "/x/passport-login/web/qrcode/generate";
    
    /** 扫码登录(web端) */
    public static final String LOGIN_QR_CODE_POLL = "/x/passport-login/web/qrcode/poll";
    
    // ==================== 其他API ====================
    /** 自定义API调用基础路径 */
    public static final String CUSTOM_API_BASE = "";
}