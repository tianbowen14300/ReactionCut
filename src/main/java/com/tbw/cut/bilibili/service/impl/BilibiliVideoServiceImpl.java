package com.tbw.cut.bilibili.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import com.tbw.cut.bilibili.BilibiliApiResponseParser;
import com.tbw.cut.bilibili.constant.BilibiliApiConstants;
import com.tbw.cut.bilibili.service.BilibiliVideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Bilibili视频相关服务实现
 */
@Slf4j
@Service
public class BilibiliVideoServiceImpl implements BilibiliVideoService {
    
    @Autowired
    private BilibiliApiClient apiClient;
    
    @Autowired
    private BilibiliApiResponseParser responseParser;
    
    @Value("${bilibili.api.base-url:https://api.bilibili.com}")
    private String baseUrl;
    
    @Override
    public JSONObject getUserInfo(String userId) {
        try {
            String url = baseUrl + BilibiliApiConstants.USER_INFO;
            Map<String, String> params = new HashMap<>();
            params.put("mid", userId);
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取用户信息失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject getVideoInfo(String bvid) {
        try {
            String url = baseUrl + BilibiliApiConstants.VIDEO_VIEW;
            Map<String, String> params = new HashMap<>();
            params.put("bvid", bvid);
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取视频信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取视频信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取视频播放信息（支持WBI签名）
     * @param bvid 视频BV号
     * @param cid 视频CID
     * @return 播放信息
     */
    @Override
    public JSONObject getVideoPlayInfo(String bvid, String cid) {
        return getVideoPlayInfo(bvid, cid, null, null, null, null);
    }
    
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
    public JSONObject getVideoPlayInfo(String bvid, String cid, String qn, String fnval, String fnver, String fourk) {
        try {
            // 使用WBI签名的URL
            String url = baseUrl + "/x/player/wbi/playurl";
            Map<String, String> params = new HashMap<>();
            params.put("bvid", bvid);
            params.put("cid", cid);
            
            // 设置默认参数
            params.put("qn", qn != null ? qn : "112"); // 默认1080P+
            params.put("fnval", fnval != null ? fnval : "1"); // 默认MP4格式，包含音频
            params.put("fnver", fnver != null ? fnver : "0");
            params.put("fourk", fourk != null ? fourk : "1");
            
            // 使用带WBI签名的请求
            String response = apiClient.getWithWbiSign(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取视频播放信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取视频播放信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取视频播放信息（支持avid）
     * @param aid 视频AV号
     * @param cid 视频CID
     * @param qn 视频清晰度标识
     * @param fnval 视频流格式标识
     * @param fnver 视频流版本标识
     * @param fourk 是否允许4K视频
     * @return 播放信息
     */
    public JSONObject getVideoPlayInfoByAid(String aid, String cid, String qn, String fnval, String fnver, String fourk) {
        try {
            // 使用WBI签名的URL
            String url = baseUrl + "/x/player/wbi/playurl";
            Map<String, String> params = new HashMap<>();
            params.put("avid", aid);
            params.put("cid", cid);
            
            // 设置默认参数
            params.put("qn", qn != null ? qn : "112"); // 默认1080P+
            params.put("fnval", fnval != null ? fnval : "1"); // 默认MP4格式，包含音频
            params.put("fnver", fnver != null ? fnver : "0");
            params.put("fourk", fourk != null ? fourk : "1");
            
            // 使用带WBI签名的请求
            String response = apiClient.getWithWbiSign(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取视频播放信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取视频播放信息失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject searchVideos(String keyword, int page, int pageSize) {
        try {
            String url = baseUrl + BilibiliApiConstants.VIDEO_SEARCH;
            Map<String, String> params = new HashMap<>();
            params.put("keyword", keyword);
            params.put("page", String.valueOf(page));
            params.put("page_size", String.valueOf(pageSize));
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("搜索视频失败: {}", e.getMessage(), e);
            throw new RuntimeException("搜索视频失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject getVideoComments(String oid, int type, int page, int pageSize) {
        try {
            String url = baseUrl + BilibiliApiConstants.VIDEO_COMMENTS;
            Map<String, String> params = new HashMap<>();
            params.put("oid", oid);
            params.put("type", String.valueOf(type));
            params.put("pn", String.valueOf(page));
            params.put("ps", String.valueOf(pageSize));
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取视频评论失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取视频评论失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject getVideoDetail(Long aid, String bvid) {
        try {
            String url = baseUrl + BilibiliApiConstants.VIDEO_VIEW;
            Map<String, String> params = new HashMap<>();
            
            if (aid != null) {
                params.put("aid", String.valueOf(aid));
            }
            
            if (StringUtils.hasText(bvid)) {
                params.put("bvid", bvid);
            }
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取视频详细信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取视频详细信息失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject getVideoDetailInfo(Long aid, String bvid, Integer needElec) {
        try {
            String url = baseUrl + BilibiliApiConstants.VIDEO_DETAIL;
            Map<String, String> params = new HashMap<>();
            
            if (aid != null) {
                params.put("aid", String.valueOf(aid));
            }
            
            if (StringUtils.hasText(bvid)) {
                params.put("bvid", bvid);
            }
            
            if (needElec != null) {
                params.put("need_elec", String.valueOf(needElec));
            }
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取视频超详细信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取视频超详细信息失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getVideoDescription(Long aid, String bvid) {
        try {
            String url = baseUrl + BilibiliApiConstants.VIDEO_DESCRIPTION;
            Map<String, String> params = new HashMap<>();
            
            if (aid != null) {
                params.put("aid", String.valueOf(aid));
            }
            
            if (StringUtils.hasText(bvid)) {
                params.put("bvid", bvid);
            }
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getDataString("data");
        } catch (Exception e) {
            log.error("获取视频简介失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取视频简介失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject getVideoPageList(Long aid, String bvid) {
        try {
            String url = baseUrl + BilibiliApiConstants.VIDEO_PAGE_LIST;
            Map<String, String> params = new HashMap<>();
            
            if (aid != null) {
                params.put("aid", String.valueOf(aid));
            }
            
            if (StringUtils.hasText(bvid)) {
                params.put("bvid", bvid);
            }
            
            String response = apiClient.get(url, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("查询视频分P列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询视频分P列表失败: " + e.getMessage(), e);
        }
    }
}