package com.tbw.cut.bilibili;

import com.tbw.cut.bilibili.service.BilibiliUnifiedService;

/**
 * Bilibili服务接口，保持向后兼容
 * 
 * @deprecated 请使用专门的服务接口：
 * - {@link com.tbw.cut.bilibili.service.BilibiliLiveService} 直播相关功能
 * - {@link com.tbw.cut.bilibili.service.BilibiliVideoService} 视频相关功能
 * - {@link com.tbw.cut.bilibili.service.BilibiliLoginService} 登录相关功能
 * - {@link com.tbw.cut.bilibili.service.BilibiliUnifiedService} 统一服务接口
 */
@Deprecated
public interface BilibiliService extends BilibiliUnifiedService {
    // 保持向后兼容，继承所有方法
}
