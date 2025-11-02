package com.tbw.cut.bilibili.service;

import com.alibaba.fastjson.JSONObject;

/**
 * Bilibili二维码登录服务接口
 */
public interface BilibiliQRCodeLoginService {
    
    /**
     * 生成二维码
     * @return 二维码信息(url和qrcode_key)
     */
    JSONObject generateQRCode();
    
    /**
     * 轮询扫码登录状态
     * @param qrcodeKey 二维码秘钥
     * @return 扫码登录结果
     */
    PollResult pollQRCodeStatus(String qrcodeKey);
    
    /**
     * 执行完整的二维码登录流程
     * @return 二维码登录结果
     */
    QRCodeLoginResult performQRCodeLogin();
    
    /**
     * 二维码登录结果
     */
    class QRCodeLoginResult {
        private boolean success;
        private String message;
        private String cookies;
        
        public QRCodeLoginResult(boolean success, String message, String cookies) {
            this.success = success;
            this.message = message;
            this.cookies = cookies;
        }
        
        // Getters and setters
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getCookies() {
            return cookies;
        }
        
        public void setCookies(String cookies) {
            this.cookies = cookies;
        }
    }
    
    /**
     * 轮询结果
     */
    class PollResult {
        private int code; // 0: 登录成功, 1: 未扫描, 2: 已扫描未确认, 3: 二维码失效
        private String message;
        private JSONObject data; // 登录成功时包含用户信息和cookie
        
        public PollResult(int code, String message, JSONObject data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }
        
        // Getters and setters
        public int getCode() {
            return code;
        }
        
        public void setCode(int code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public JSONObject getData() {
            return data;
        }
        
        public void setData(JSONObject data) {
            this.data = data;
        }
    }
}