package com.tbw.cut.controller;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.service.BilibiliLoginService;
import com.tbw.cut.bilibili.service.BilibiliQRCodeLoginService;
import com.tbw.cut.dto.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

/**
 * Bilibili二维码登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/bilibili/qrcode")
public class BilibiliQRCodeLoginController {
    
    @Autowired
    @Qualifier("bilibiliLoginServiceImpl")
    private BilibiliLoginService bilibiliLoginService;
    
    @Autowired
    private BilibiliQRCodeLoginService bilibiliQRCodeLoginService;
    
    /**
     * 申请二维码
     * @return 二维码信息
     */
    @GetMapping("/generate")
    public ResponseResult<JSONObject> generateQRCode() {
        try {
            JSONObject qrCodeData = bilibiliLoginService.generateQRCode();
            return ResponseResult.success(qrCodeData);
        } catch (Exception e) {
            log.error("申请二维码失败", e);
            return ResponseResult.error("申请二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 轮询扫码登录状态
     * @param qrcodeKey 二维码秘钥
     * @return 扫码登录结果
     */
    @GetMapping("/poll")
    public ResponseResult<BilibiliQRCodeLoginService.PollResult> pollQRCodeLogin(@RequestParam String qrcodeKey) {
        try {
            // 使用新的轮询方法
            BilibiliQRCodeLoginService.PollResult pollResult = bilibiliQRCodeLoginService.pollQRCodeStatus(qrcodeKey);
            return ResponseResult.success(pollResult);
        } catch (Exception e) {
            log.error("轮询扫码登录状态失败", e);
            return ResponseResult.error("轮询扫码登录状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行完整的二维码登录流程（异步）
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseResult<String> performQRCodeLogin() {
        try {
            // 启动一个新的线程执行登录流程，避免阻塞主线程
            new Thread(() -> {
                BilibiliQRCodeLoginService.QRCodeLoginResult result = bilibiliQRCodeLoginService.performQRCodeLogin();
                log.info("二维码登录结果: success={}, message={}", result.isSuccess(), result.getMessage());
            }).start();
            
            return ResponseResult.success("二维码登录流程已启动，请查看控制台输出");
        } catch (Exception e) {
            log.error("执行二维码登录流程失败", e);
            return ResponseResult.error("执行二维码登录流程失败: " + e.getMessage());
        }
    }
}