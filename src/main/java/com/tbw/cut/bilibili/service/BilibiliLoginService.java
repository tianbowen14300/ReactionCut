package com.tbw.cut.bilibili.service;

import com.alibaba.fastjson.JSONObject;

/**
 * Bilibili登录相关服务接口
 */
public interface BilibiliLoginService {
    
    /**
     * 获取Web端登录公钥和盐值
     * @return 公钥和盐值信息
     */
    JSONObject getWebLoginKey();
    
    /**
     * Web端密码登录
     * @param username 用户名
     * @param password 加密后的带盐密码
     * @param token 登录token
     * @param challenge 极验challenge
     * @param validate 极验result
     * @param seccode 极验result +|jordan
     * @param keep 是否保持登录
     * @param source 登录来源
     * @param goUrl 跳转URL
     * @return 登录结果
     */
    JSONObject webLogin(String username, String password, String token, String challenge, 
                       String validate, String seccode, int keep, String source, String goUrl);
    
    /**
     * 申请二维码(web端)
     * @return 二维码信息(url和qrcode_key)
     */
    JSONObject generateQRCode();
    
    /**
     * 扫码登录(web端)
     * @param qrcodeKey 扫码登录秘钥
     * @return 扫码登录结果
     */
    JSONObject pollQRCodeLogin(String qrcodeKey);
}