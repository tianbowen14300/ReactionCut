package com.tbw.cut.bilibili.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import com.tbw.cut.bilibili.BilibiliApiResponseParser;
import com.tbw.cut.bilibili.constant.BilibiliApiConstants;
import com.tbw.cut.bilibili.service.BilibiliLoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Bilibili登录相关服务实现
 */
@Slf4j
@Service
public class BilibiliLoginServiceImpl implements BilibiliLoginService {
    
    @Autowired
    private BilibiliApiClient apiClient;
    
    @Autowired
    private BilibiliApiResponseParser responseParser;
    
    @Value("${bilibili.api.base-url:https://api.bilibili.com}")
    private String baseUrl;
    
    @Value("${bilibili.passport.base-url:https://passport.bilibili.com}")
    private String passportBaseUrl;
    
    @Override
    public JSONObject getWebLoginKey() {
        try {
            String url = baseUrl + BilibiliApiConstants.LOGIN_WEB_KEY;
            String response = apiClient.get(url, null);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("获取Web端登录公钥和盐值失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取Web端登录公钥和盐值失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject webLogin(String username, String password, String token, String challenge, 
                              String validate, String seccode, int keep, String source, String goUrl) {
        try {
            String url = baseUrl + BilibiliApiConstants.LOGIN_WEB;
            Map<String, String> params = new HashMap<>();
            params.put("username", username);
            params.put("password", password);
            params.put("keep", String.valueOf(keep));
            params.put("token", token);
            params.put("challenge", challenge);
            params.put("validate", validate);
            params.put("seccode", seccode);
            
            if (StringUtils.hasText(source)) {
                params.put("source", source);
            }
            
            if (StringUtils.hasText(goUrl)) {
                params.put("go_url", goUrl);
            }
            
            String response = apiClient.post(url, null, params);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("Web端密码登录失败: {}", e.getMessage(), e);
            throw new RuntimeException("Web端密码登录失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject generateQRCode() {
        try {
            String url = passportBaseUrl + BilibiliApiConstants.LOGIN_QR_CODE_GENERATE;
            String response = apiClient.get(url, null);
            return responseParser.parseAndCheck(response).getData();
        } catch (Exception e) {
            log.error("申请二维码失败: {}", e.getMessage(), e);
            throw new RuntimeException("申请二维码失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject pollQRCodeLogin(String qrcodeKey) {
        try {
            String url = passportBaseUrl + BilibiliApiConstants.LOGIN_QR_CODE_POLL;
            Map<String, String> params = new HashMap<>();
            params.put("qrcode_key", qrcodeKey);
            params.put("source", "main-fe-header");

            log.info("准备轮询二维码状态，URL: {}, qrcodeKey: {}", url, qrcodeKey);
            
            String response = apiClient.get(url, params);
            log.debug("Bilibili API响应: {}", response);
            
            JSONObject result = responseParser.parseAndCheck(response).getData();
            log.info("解析后的轮询结果: {}", result != null ? result.toJSONString() : "null");
            
            return result;
        } catch (Exception e) {
            log.error("扫码登录失败: {}", e.getMessage(), e);
            throw new RuntimeException("扫码登录失败: " + e.getMessage(), e);
        }
    }
}