package com.tbw.cut;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import com.tbw.cut.bilibili.BilibiliApiResponse;
import com.tbw.cut.bilibili.BilibiliApiResponseParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class BilibiliQRCodeTest {

    @Autowired
    private BilibiliApiClient apiClient;

    @Autowired
    private BilibiliApiResponseParser responseParser;

    @Test
    public void testGenerateQRCode() {
        try {
            // 测试生成二维码
            String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";
            String response = apiClient.get(url, null);
            System.out.println("二维码生成响应: " + response);

            BilibiliApiResponse apiResponse = responseParser.parse(response);
            if (apiResponse.isSuccess()) {
                JSONObject data = apiResponse.getData();
                System.out.println("二维码数据: " + data.toJSONString());
                
                if (data.containsKey("url") && data.containsKey("qrcode_key")) {
                    System.out.println("二维码URL: " + data.getString("url"));
                    System.out.println("二维码Key: " + data.getString("qrcode_key"));
                } else {
                    System.out.println("返回数据格式不正确");
                }
            } else {
                System.out.println("请求失败: " + apiResponse.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPollQRCodeStatus() {
        try {
            // 先生成二维码获取qrcode_key
            String generateUrl = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";
            String generateResponse = apiClient.get(generateUrl, null);
            BilibiliApiResponse generateApiResponse = responseParser.parse(generateResponse);
            
            if (generateApiResponse.isSuccess()) {
                JSONObject data = generateApiResponse.getData();
                String qrcodeKey = data.getString("qrcode_key");
                
                System.out.println("二维码Key: " + qrcodeKey);
                
                // 轮询二维码状态
                String pollUrl = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll";
                Map<String, String> params = new HashMap<>();
                params.put("qrcode_key", qrcodeKey);
                
                String pollResponse = apiClient.get(pollUrl, params);
                System.out.println("轮询响应: " + pollResponse);
                
                BilibiliApiResponse pollApiResponse = responseParser.parse(pollResponse);
                if (pollApiResponse.isSuccess()) {
                    JSONObject pollData = pollApiResponse.getData();
                    System.out.println("轮询数据: " + pollData.toJSONString());
                } else {
                    System.out.println("轮询失败: " + pollApiResponse.getMessage());
                }
            } else {
                System.out.println("生成二维码失败: " + generateApiResponse.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}