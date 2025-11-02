package com.tbw.cut.bilibili.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Bilibili WBI签名工具类
 * 参考：https://github.com/SocialSisterYi/bilibili-API-collect/issues/1107
 */
@Slf4j
@Component
public class WbiSignUtils {
    
    private static final int[] MIXIN_KEY_ENC_TAB = {
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52
    };
    
    private String imgKey = "";
    private String subKey = "";
    private long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 1000 * 60 * 10; // 10分钟更新一次
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @PostConstruct
    public void init() {
        updateWbiKeys();
    }
    
    /**
     * 更新WBI密钥
     */
    public synchronized void updateWbiKeys() {
        long now = System.currentTimeMillis();
        // 如果距离上次更新不到10分钟，则不更新
        if (now - lastUpdate < UPDATE_INTERVAL) {
            return;
        }
        
        try {
            String url = "https://api.bilibili.com/x/web-interface/nav";
            String response = restTemplate.getForObject(url, String.class);
            
            // 简化的JSON解析，实际项目中应该使用fastjson或其他JSON库
            // 这里为了简化实现，使用字符串查找方式
            String imgKeyStr = extractJsonValue(response, "img_url");
            String subKeyStr = extractJsonValue(response, "sub_url");
            
            if (imgKeyStr != null && subKeyStr != null) {
                imgKey = extractKeyFromUrl(imgKeyStr);
                subKey = extractKeyFromUrl(subKeyStr);
                lastUpdate = now;
                log.info("WBI密钥更新成功: imgKey={}, subKey={}", imgKey, subKey);
            }
        } catch (Exception e) {
            log.error("更新WBI密钥失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从URL中提取密钥
     */
    private String extractKeyFromUrl(String url) {
        int lastSlash = url.lastIndexOf('/');
        int lastDot = url.lastIndexOf('.');
        if (lastSlash != -1 && lastDot != -1 && lastDot > lastSlash) {
            return url.substring(lastSlash + 1, lastDot);
        }
        return "";
    }
    
    /**
     * 简单的JSON值提取方法
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start != -1) {
            start += searchKey.length();
            int end = json.indexOf("\"", start);
            if (end != -1) {
                return json.substring(start, end);
            }
        }
        return null;
    }
    
    /**
     * 获取MixinKey
     */
    private String getMixinKey() {
        String mixinKey = imgKey + subKey;
        StringBuilder result = new StringBuilder();
        for (int i : MIXIN_KEY_ENC_TAB) {
            if (i < mixinKey.length()) {
                result.append(mixinKey.charAt(i));
            }
        }
        return result.substring(0, Math.min(32, result.length()));
    }
    
    /**
     * MD5加密
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }
    
    /**
     * 对参数进行WBI签名
     * @param params 参数Map
     * @return 签名后的参数字符串
     */
    public String signParams(Map<String, String> params) {
        // 确保密钥是最新的
        updateWbiKeys();
        
        // 添加必要的时间戳参数
        Map<String, String> signedParams = new LinkedHashMap<>(params);
        signedParams.put("wts", String.valueOf(System.currentTimeMillis() / 1000));
        
        // 添加WebGL指纹参数（简化处理）
        signedParams.put("dm_img_list", "[]");
        signedParams.put("dm_img_str", "");
        signedParams.put("dm_cover_img_str", "");
        
        // 过滤特殊字符
        Map<String, String> filteredParams = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : signedParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // 过滤特殊字符
            value = value.replaceAll("[!'()*]", "");
            filteredParams.put(key, value);
        }
        
        // 按key排序
        List<String> paramList = new ArrayList<>();
        for (Map.Entry<String, String> entry : filteredParams.entrySet()) {
            paramList.add(entry.getKey() + "=" + entry.getValue());
        }
        Collections.sort(paramList);
        
        // 构造查询字符串
        String query = String.join("&", paramList);
        
        // 计算WBI签名
        String mixinKey = getMixinKey();
        String sign = md5(query + mixinKey);
        
        // 返回完整参数字符串
        return query + "&w_rid=" + sign;
    }
}