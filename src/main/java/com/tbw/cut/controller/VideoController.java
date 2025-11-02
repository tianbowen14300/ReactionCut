package com.tbw.cut.controller;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import com.tbw.cut.bilibili.service.BilibiliVideoService;
import com.tbw.cut.dto.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    @Qualifier("bilibiliVideoServiceImpl")
    private BilibiliVideoService bilibiliVideoService;

    @Autowired
    private BilibiliApiClient bilibiliApiClient;

    /**
     * 获取视频详细信息
     * @param bvid BV号
     * @return 视频详细信息
     */
    @GetMapping("/detail")
    public ResponseResult<JSONObject> getVideoDetail(@RequestParam(required = false) String bvid,
                                                     @RequestParam(required = false) Long aid) {
        try {
            log.info("获取视频详细信息: bvid={}, aid={}", bvid, aid);
            
            if (bvid == null && aid == null) {
                return ResponseResult.error("BV号或AV号不能为空");
            }
            
            JSONObject data = bilibiliVideoService.getVideoDetail(aid, bvid);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取视频详细信息失败: bvid={}, aid={}", bvid, aid, e);
            return ResponseResult.error("获取视频详细信息失败: " + e.getMessage());
        }
    }

    /**
     * 代理获取B站图片
     * @param imageUrl 图片URL
     * @return 图片数据
     */
    @GetMapping("/proxy-image")
    public void proxyBilibiliImage(@RequestParam String imageUrl, HttpServletResponse response) {
        try {
            log.info("代理获取B站图片: {}", imageUrl);
            
            // 创建HTTP客户端
            RestTemplate restTemplate = new RestTemplate();
            
            // 添加请求头，模拟浏览器访问
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Referer", "https://www.bilibili.com");
            
            // 如果有登录信息，添加Cookie
            try {
                String cookie = bilibiliApiClient.extractCookieFromLoginInfo();
                if (cookie != null && !cookie.isEmpty()) {
                    headers.set("Cookie", cookie);
                    log.debug("添加Cookie头到图片请求");
                }
            } catch (Exception e) {
                log.warn("获取Cookie信息失败: {}", e.getMessage());
            }
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            // 发送请求获取图片
            ResponseEntity<byte[]> imageResponse = restTemplate.exchange(
                imageUrl, HttpMethod.GET, entity, byte[].class);
            
            // 设置响应头
            MediaType contentType = imageResponse.getHeaders().getContentType();
            if (contentType != null) {
                response.setContentType(contentType.toString());
            } else {
                response.setContentType("image/jpeg");
            }
            response.setContentLength(imageResponse.getBody().length);
            
            // 写入图片数据
            response.getOutputStream().write(imageResponse.getBody());
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            log.error("代理获取B站图片失败: {}", e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "获取图片失败");
            } catch (Exception ex) {
                log.error("发送错误响应失败: {}", ex.getMessage(), ex);
            }
        }
    }
}