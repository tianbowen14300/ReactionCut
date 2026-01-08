package com.tbw.cut.bilibili;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BilibiliApiResponseParser {
    
    /**
     * 解析Bilibili API响应
     * @param response 响应字符串
     * @return BilibiliApiResponse对象
     */
    public BilibiliApiResponse parse(String response) {
        try {
            BilibiliApiResponse apiResponse = new BilibiliApiResponse();
            
            if (response == null || response.isEmpty()) {
                apiResponse.setCode(-1);
                apiResponse.setMessage("响应为空");
                return apiResponse;
            }
            
            JSONObject jsonObject = JSON.parseObject(response);
            
            if (jsonObject.containsKey("code")) {
                apiResponse.setCode(jsonObject.getInteger("code"));
            }
            
            if (jsonObject.containsKey("message")) {
                apiResponse.setMessage(jsonObject.getString("message"));
            }
            
            if (jsonObject.containsKey("ttl")) {
                apiResponse.setTtl(jsonObject.getInteger("ttl"));
            }
            
            if (jsonObject.containsKey("data")) {
                Object data = jsonObject.get("data");
                if (data instanceof JSONObject) {
                    apiResponse.setData((JSONObject) data);
                } else {
                    // 如果data不是JSONObject，将其转换为JSONObject
                    JSONObject dataObject = new JSONObject();
                    dataObject.put("value", data);
                    apiResponse.setData(dataObject);
                }
            }
            
            return apiResponse;
        } catch (Exception e) {
            log.error("解析Bilibili API响应失败: {}", e.getMessage(), e);
            
            BilibiliApiResponse apiResponse = new BilibiliApiResponse();
            apiResponse.setCode(-1);
            apiResponse.setMessage("解析响应失败: " + e.getMessage());
            return apiResponse;
        }
    }
    
    /**
     * 解析Bilibili API响应并检查是否成功
     * @param response 响应字符串
     * @return BilibiliApiResponse对象
     * @throws RuntimeException 如果请求失败
     */
    public BilibiliApiResponse parseAndCheck(String response) throws RuntimeException {
        BilibiliApiResponse apiResponse = parse(response);
        
        if (!apiResponse.isSuccess()) {
            log.error("API请求失败 - code: {}, message: {}", apiResponse.getCode(), apiResponse.getMessage());
            throw new RuntimeException("Bilibili API请求失败: " + apiResponse.getMessage() + 
                                     " (code: " + apiResponse.getCode() + ")");
        }
        
        return apiResponse;
    }
}