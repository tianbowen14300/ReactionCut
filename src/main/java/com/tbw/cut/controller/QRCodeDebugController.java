package com.tbw.cut.controller;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import com.tbw.cut.bilibili.BilibiliApiResponse;
import com.tbw.cut.bilibili.BilibiliApiResponseParser;
import com.tbw.cut.bilibili.constant.BilibiliApiConstants;
import com.tbw.cut.dto.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/debug/qrcode")
public class QRCodeDebugController {

    @Autowired
    private BilibiliApiClient apiClient;

    @Autowired
    private BilibiliApiResponseParser responseParser;

    @Value("${bilibili.passport.base-url:https://passport.bilibili.com}")
    private String passportBaseUrl;

    @GetMapping("/generate")
    public ResponseResult<JSONObject> debugGenerateQRCode() {
        try {
            log.info("调试生成Bilibili二维码");
            String url = passportBaseUrl + BilibiliApiConstants.LOGIN_QR_CODE_GENERATE;
            log.info("请求URL: {}", url);
            
            String response = apiClient.get(url, null);
            log.info("Bilibili API响应: {}", response);
            
            BilibiliApiResponse apiResponse = responseParser.parse(response);
            log.info("解析后的响应: code={}, message={}", apiResponse.getCode(), apiResponse.getMessage());
            
            if (apiResponse.isSuccess()) {
                JSONObject data = apiResponse.getData();
                log.info("二维码数据: {}", data);
                
                if (data != null) {
                    log.info("URL: {}", data.getString("url"));
                    log.info("二维码Key: {}", data.getString("qrcode_key"));
                }
                
                return ResponseResult.success(data);
            } else {
                return ResponseResult.error("API请求失败: " + apiResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("调试生成二维码失败", e);
            return ResponseResult.error("调试生成二维码失败: " + e.getMessage());
        }
    }
}