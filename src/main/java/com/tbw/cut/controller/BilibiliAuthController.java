package com.tbw.cut.controller;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.service.BilibiliQRCodeLoginService;
import com.tbw.cut.bilibili.service.impl.BilibiliQRCodeLoginServiceImpl;
import com.tbw.cut.dto.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Bilibili认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/bilibili/auth")
public class BilibiliAuthController {
    
    @Autowired
    private BilibiliQRCodeLoginService bilibiliQRCodeLoginService;
    
    /**
     * 生成二维码
     * @return 二维码信息
     */
    @GetMapping("/qrcode/generate")
    public ResponseResult<JSONObject> generateQRCode() {
        try {
            JSONObject qrCodeData = bilibiliQRCodeLoginService.generateQRCode();
            return ResponseResult.success(qrCodeData);
        } catch (Exception e) {
            log.error("生成二维码失败", e);
            return ResponseResult.error("生成二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 轮询二维码状态
     * @param qrcodeKey 二维码秘钥
     * @return 轮询结果
     */
    @GetMapping("/qrcode/poll")
    public ResponseResult<BilibiliQRCodeLoginService.PollResult> pollQRCodeStatus(@RequestParam String qrcodeKey) {
        try {
            log.info("接收到轮询请求，qrcodeKey: {}", qrcodeKey);
            BilibiliQRCodeLoginService.PollResult result = 
                bilibiliQRCodeLoginService.pollQRCodeStatus(qrcodeKey);
            log.info("轮询结果: code={}, message={}", result.getCode(), result.getMessage());
            
            // 添加更多调试信息
            if (result.getData() != null) {
                log.debug("轮询详细数据: {}", result.getData().toJSONString());
            }
            
            return ResponseResult.success(result);
        } catch (Exception e) {
            log.error("轮询二维码状态失败", e);
            return ResponseResult.error("轮询二维码状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查登录状态
     * @return 登录状态
     */
    @GetMapping("/status")
    public ResponseResult<Map<String, Object>> checkLoginStatus() {
        try {
            Map<String, Object> responseData = new HashMap<>();
            boolean loggedIn = ((BilibiliQRCodeLoginServiceImpl) bilibiliQRCodeLoginService).isLoggedIn();
            responseData.put("loggedIn", loggedIn);
            
            if (loggedIn) {
                // 获取用户信息
                JSONObject loginInfo = ((BilibiliQRCodeLoginServiceImpl) bilibiliQRCodeLoginService).getLoginInfo();
                if (loginInfo != null) {
                    responseData.put("userInfo", loginInfo.getJSONObject("data"));
                }
            }
            
            return ResponseResult.success(responseData);
        } catch (Exception e) {
            log.error("检查登录状态失败", e);
            return ResponseResult.error("检查登录状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 注销登录
     * @return 注销结果
     */
    @PostMapping("/logout")
    public ResponseResult<String> logout() {
        try {
            ((BilibiliQRCodeLoginServiceImpl) bilibiliQRCodeLoginService).logout();
            return ResponseResult.success("注销成功");
        } catch (Exception e) {
            log.error("注销登录失败", e);
            return ResponseResult.error("注销登录失败: " + e.getMessage());
        }
    }
}