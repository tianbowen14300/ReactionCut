package com.tbw.cut.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import com.tbw.cut.bilibili.BilibiliApiResponse;
import com.tbw.cut.bilibili.BilibiliApiResponseParser;
import com.tbw.cut.bilibili.service.BilibiliVideoService;
import com.tbw.cut.dto.ResponseResult;
import com.tbw.cut.entity.LoginInfo;
import com.tbw.cut.mapper.LoginInfoMapper;
import com.tbw.cut.service.LoginInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private BilibiliApiClient apiClient;

    @Autowired
    private BilibiliApiResponseParser responseParser;
    
    @Autowired
    private LoginInfoService loginInfoService;
    
    @Autowired
    @Qualifier("bilibiliVideoServiceImpl")
    private BilibiliVideoService bilibiliVideoService;

    @GetMapping("/qrcode")
    public ResponseResult<JSONObject> testQRCode() {
        try {
            // 测试生成二维码
            String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";
            String response = apiClient.get(url, null);
            log.info("二维码生成响应: {}", response);

            BilibiliApiResponse apiResponse = responseParser.parse(response);
            if (apiResponse.isSuccess()) {
                JSONObject data = apiResponse.getData();
                log.info("二维码数据: {}", data.toJSONString());
                
                if (data.containsKey("url") && data.containsKey("qrcode_key")) {
                    log.info("二维码URL: {}", data.getString("url"));
                    log.info("二维码Key: {}", data.getString("qrcode_key"));
                    return ResponseResult.success(data);
                } else {
                    log.error("返回数据格式不正确: {}", data.toJSONString());
                    return ResponseResult.error("返回数据格式不正确");
                }
            } else {
                log.error("请求失败: {}", apiResponse.getMessage());
                return ResponseResult.error("请求失败: " + apiResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("生成二维码测试失败", e);
            return ResponseResult.error("生成二维码测试失败: " + e.getMessage());
        }
    }

    @GetMapping("/poll")
    public ResponseResult<JSONObject> testPollQRCode(String qrcodeKey) {
        try {
            if (qrcodeKey == null || qrcodeKey.isEmpty()) {
                return ResponseResult.error("二维码Key不能为空");
            }
            
            // 轮询二维码状态
            String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll";
            Map<String, String> params = new HashMap<>();
            params.put("qrcode_key", qrcodeKey);
            
            String response = apiClient.get(url, params);
            log.info("轮询响应: {}", response);
            
            BilibiliApiResponse apiResponse = responseParser.parse(response);
            if (apiResponse.isSuccess()) {
                JSONObject data = apiResponse.getData();
                log.info("轮询数据: {}", data.toJSONString());
                return ResponseResult.success(data);
            } else {
                log.error("轮询失败: {}", apiResponse.getMessage());
                return ResponseResult.error("轮询失败: " + apiResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("轮询二维码状态测试失败", e);
            return ResponseResult.error("轮询二维码状态测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试Bilibili二维码轮询API实际响应格式
     * @param qrcodeKey 二维码秘钥
     * @return 轮询结果
     */
    @GetMapping("/poll/debug")
    public ResponseResult<JSONObject> testPollQRCodeDebug(String qrcodeKey) {
        try {
            if (qrcodeKey == null || qrcodeKey.isEmpty()) {
                return ResponseResult.error("二维码Key不能为空");
            }
            
            // 轮询二维码状态
            String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll";
            Map<String, String> params = new HashMap<>();
            params.put("qrcode_key", qrcodeKey);
            
            String response = apiClient.get(url, params);
            log.info("轮询响应(原始): {}", response);
            
            // 解析响应
            BilibiliApiResponse apiResponse = responseParser.parse(response);
            log.info("解析后的API响应: code={}, message={}, ttl={}", 
                    apiResponse.getCode(), apiResponse.getMessage(), apiResponse.getTtl());
            
            if (apiResponse.getData() != null) {
                log.info("解析后的数据字段: {}", apiResponse.getData().toJSONString());
                // 检查data字段中是否包含code和message
                JSONObject data = apiResponse.getData();
                if (data.containsKey("code")) {
                    log.info("data.code = {}", data.getIntValue("code"));
                }
                if (data.containsKey("message")) {
                    log.info("data.message = {}", data.getString("message"));
                }
            }
            
            // 直接返回原始响应字符串，便于调试
            JSONObject result = new JSONObject();
            result.put("rawResponse", response);
            result.put("parsedResponse", JSON.parseObject(JSON.toJSONString(apiResponse)));
            
            return ResponseResult.success(result);
        } catch (Exception e) {
            log.error("轮询二维码状态调试测试失败", e);
            return ResponseResult.error("轮询二维码状态调试测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试Bilibili二维码登录完整流程
     * @return 登录结果
     */
    @GetMapping("/qrcode/login/debug")
    public ResponseResult<JSONObject> testQRCodeLoginDebug() {
        try {
            log.info("开始测试二维码登录完整流程");
            
            // 1. 生成二维码
            String generateUrl = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";
            String generateResponse = apiClient.get(generateUrl, null);
            log.info("生成二维码响应: {}", generateResponse);
            
            BilibiliApiResponse generateApiResponse = responseParser.parse(generateResponse);
            if (!generateApiResponse.isSuccess()) {
                return ResponseResult.error("生成二维码失败: " + generateApiResponse.getMessage());
            }
            
            JSONObject qrData = generateApiResponse.getData();
            String qrcodeKey = qrData.getString("qrcode_key");
            log.info("二维码Key: {}", qrcodeKey);
            
            // 2. 轮询状态（仅轮询几次用于测试）
            String pollUrl = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll";
            Map<String, String> params = new HashMap<>();
            params.put("qrcode_key", qrcodeKey);
            
            // 轮询3次查看响应格式
            for (int i = 0; i < 3; i++) {
                Thread.sleep(2000); // 等待2秒
                String pollResponse = apiClient.get(pollUrl, params);
                log.info("第{}次轮询响应: {}", i+1, pollResponse);
                
                BilibiliApiResponse pollApiResponse = responseParser.parse(pollResponse);
                if (pollApiResponse.isSuccess() && pollApiResponse.getData() != null) {
                    JSONObject pollData = pollApiResponse.getData();
                    log.info("第{}次轮询数据: code={}, message={}", i+1, 
                            pollData.getIntValue("code"), pollData.getString("message"));
                    
                    // 如果登录成功，记录完整响应
                    if (pollData.getIntValue("code") == 0) {
                        log.info("登录成功，完整响应数据: {}", pollResponse);
                        JSONObject result = new JSONObject();
                        result.put("qrCodeData", qrData);
                        result.put("loginResponse", pollResponse);
                        result.put("loginData", pollData);
                        return ResponseResult.success(result);
                    }
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("qrCodeData", qrData);
            result.put("message", "测试完成，未检测到登录成功");
            return ResponseResult.success(result);
        } catch (Exception e) {
            log.error("二维码登录完整流程测试失败", e);
            return ResponseResult.error("二维码登录完整流程测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试获取视频详细信息
     * @param bvid 视频BV号
     * @return 视频详细信息
     */
    @GetMapping("/video/detail")
    public ResponseResult<JSONObject> testGetVideoDetail(@RequestParam String bvid) {
        try {
            log.info("测试获取视频详细信息: bvid={}", bvid);
            JSONObject data = bilibiliVideoService.getVideoDetail(null, bvid);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取视频详细信息失败: bvid={}", bvid, e);
            return ResponseResult.error("获取视频详细信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 直接调用Bilibili API测试获取视频详细信息
     * @param bvid 视频BV号
     * @return 视频详细信息
     */
    @GetMapping("/video/detail/direct")
    public ResponseResult<JSONObject> testGetVideoDetailDirect(@RequestParam String bvid) {
        try {
            log.info("直接调用Bilibili API测试获取视频详细信息: bvid={}", bvid);
            
            String url = "https://api.bilibili.com/x/web-interface/view";
            Map<String, String> params = new HashMap<>();
            params.put("bvid", bvid);
            
            String response = apiClient.get(url, params);
            log.info("Bilibili API响应: {}", response);
            
            BilibiliApiResponse apiResponse = responseParser.parse(response);
            if (apiResponse.isSuccess()) {
                return ResponseResult.success(apiResponse.getData());
            } else {
                return ResponseResult.error("Bilibili API调用失败: " + apiResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("直接调用Bilibili API获取视频详细信息失败: bvid={}", bvid, e);
            return ResponseResult.error("直接调用Bilibili API获取视频详细信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试不带认证的视频信息获取
     * @param bvid 视频BV号
     * @return 视频详细信息
     */
    @GetMapping("/video/detail/noauth")
    public ResponseResult<JSONObject> testGetVideoDetailNoAuth(@RequestParam String bvid) {
        try {
            log.info("测试不带认证的视频信息获取: bvid={}", bvid);
            
            // 创建一个不添加认证信息的客户端
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            String url = "https://api.bilibili.com/x/web-interface/view";
            Map<String, String> params = new HashMap<>();
            params.put("bvid", bvid);
            
            // 构建完整URL
            StringBuilder fullUrl = new StringBuilder(url);
            if (params != null && !params.isEmpty()) {
                boolean hasQuery = url.contains("?");
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (hasQuery) {
                        fullUrl.append("&");
                    } else {
                        fullUrl.append("?");
                        hasQuery = true;
                    }
                    fullUrl.append(entry.getKey()).append("=").append(entry.getValue());
                }
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                fullUrl.toString(), HttpMethod.GET, entity, String.class);
            
            String response = responseEntity.getBody();
            log.info("不带认证的Bilibili API响应: {}", response);
            
            BilibiliApiResponse apiResponse = responseParser.parse(response);
            if (apiResponse.isSuccess()) {
                return ResponseResult.success(apiResponse.getData());
            } else {
                return ResponseResult.error("Bilibili API调用失败: " + apiResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("不带认证的视频信息获取失败: bvid={}", bvid, e);
            return ResponseResult.error("不带认证的视频信息获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试完整的视频信息获取流程
     * @param bvid 视频BV号
     * @return 视频详细信息
     */
    @GetMapping("/video/detail/full")
    public ResponseResult<JSONObject> testGetVideoDetailFull(@RequestParam String bvid) {
        try {
            log.info("测试完整的视频信息获取流程: bvid={}", bvid);
            
            // 1. 检查登录信息文件是否存在
            File loginInfoFile = new File(System.getProperty("user.dir") + "/bilibili_login_info.json");
            log.info("本地登录信息文件是否存在: {}", loginInfoFile.exists());
            
            if (loginInfoFile.exists()) {
                // 读取登录信息
                String content = new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir") + "/bilibili_login_info.json")));
                log.info("本地登录信息内容: {}", content);
            } else {
                // 检查数据库中是否存在登录信息
                log.info("本地文件不存在，检查数据库中是否存在登录信息");
                // 这里需要根据实际业务逻辑确定如何获取用户ID
                Long userId = 1L; // 默认用户ID，实际应用中需要动态获取
                if (loginInfoService.existsByUserId(userId)) {
                    log.info("数据库中存在用户 {} 的登录信息", userId);
                } else {
                    log.info("数据库中不存在用户 {} 的登录信息", userId);
                }
            }
            
            // 2. 调用视频服务
            JSONObject data = bilibiliVideoService.getVideoDetail(null, bvid);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("完整的视频信息获取流程失败: bvid={}", bvid, e);
            return ResponseResult.error("完整的视频信息获取流程失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试登录信息服务
     * @return 测试结果
     */
    @GetMapping("/login-info/test")
    public ResponseResult<String> testLoginInfoService() {
        try {
            log.info("测试登录信息服务");
            
            // 创建测试登录信息
            LoginInfo loginInfo = new LoginInfo();
            loginInfo.setUserId(12345L);
            loginInfo.setUsername("testuser");
            loginInfo.setNickname("测试用户");
            loginInfo.setAvatarUrl("https://example.com/avatar.jpg");
            loginInfo.setCookieInfo("{\"cookie\":\"test_cookie_value\"}");
            loginInfo.setLoginTime(LocalDateTime.now());
            loginInfo.setExpireTime(LocalDateTime.now().plusHours(24));
            
            // 保存到数据库
            LoginInfo savedInfo = loginInfoService.saveOrUpdate(loginInfo);
            log.info("保存登录信息成功，ID: {}", savedInfo.getId());
            
            // 查询数据库
            LoginInfo foundInfo = loginInfoService.getByUserId(12345L);
            if (foundInfo != null) {
                log.info("查询登录信息成功: userId={}, username={}", 
                        foundInfo.getUserId(), foundInfo.getUsername());
                return ResponseResult.success("测试成功，用户ID: " + foundInfo.getUserId());
            } else {
                log.warn("未找到登录信息");
                return ResponseResult.error("未找到登录信息");
            }
        } catch (Exception e) {
            log.error("测试登录信息服务失败: {}", e.getMessage(), e);
            return ResponseResult.error("测试失败: " + e.getMessage());
        }
    }
}