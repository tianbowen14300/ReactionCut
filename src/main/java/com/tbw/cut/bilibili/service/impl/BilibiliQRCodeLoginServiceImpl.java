package com.tbw.cut.bilibili.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.service.BilibiliLoginService;
import com.tbw.cut.bilibili.service.BilibiliQRCodeLoginService;
import com.tbw.cut.entity.LoginInfo;
import com.tbw.cut.mapper.LoginInfoMapper;
import com.tbw.cut.service.LoginInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class BilibiliQRCodeLoginServiceImpl implements BilibiliQRCodeLoginService {
    
    @Autowired
    @Qualifier("bilibiliLoginServiceImpl")
    private BilibiliLoginService bilibiliLoginService;
    
    @Autowired
    private LoginInfoService loginInfoService;
    
    // 登录信息存储文件路径 - 使用绝对路径确保文件不会丢失
    private static final String LOGIN_INFO_FILE = System.getProperty("user.dir") + "/bilibili_login_info.json";
    
    @Override
    public JSONObject generateQRCode() {
        try {
            log.info("生成Bilibili二维码");
            return bilibiliLoginService.generateQRCode();
        } catch (Exception e) {
            log.error("生成二维码失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成二维码失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public PollResult pollQRCodeStatus(String qrcodeKey) {
        try {
            log.info("轮询二维码状态: {}", qrcodeKey);
            // 直接调用bilibiliLoginService.pollQRCodeLogin
            JSONObject pollResult = bilibiliLoginService.pollQRCodeLogin(qrcodeKey);
            
            log.debug("Bilibili API响应: {}", pollResult != null ? pollResult.toJSONString() : "null");
            
            if (pollResult == null) {
                log.warn("轮询结果为空，返回未扫码状态");
                return new PollResult(86101, "未扫码", null);
            }
            
            // 检查外层API调用是否成功
            int apiCode = pollResult.getIntValue("code");
            log.debug("外层API调用结果 - Code: {}, Response: {}", apiCode, pollResult.toJSONString());
            
            if (apiCode != 0) {
                log.warn("外层API调用失败，返回未扫码状态");
                return new PollResult(86101, "未扫码", null);
            }
            
            // 注意：pollQRCodeLogin已经返回了data对象，不需要再次获取data字段
            // 获取内层二维码状态
            // JSONObject data = pollResult.getJSONObject("data"); // 错误的做法
            JSONObject data = pollResult; // 正确的做法，pollResult就是data对象
            
            if (data == null) {
                log.warn("轮询数据为空，返回未扫码状态");
                return new PollResult(86101, "未扫码", null);
            }
            
            // 检查内层状态码
            int code = data.getIntValue("code");
            String message = data.getString("message");
            
            log.info("二维码状态 - Code: {}, Message: {}", code, message);
            
            // 特殊处理：如果返回的状态是未扫码，但可能是因为服务器延迟，我们稍作延迟再尝试一次
            if (code == 86101) {
                log.info("检测到未扫码状态，等待1秒后重试一次");
                Thread.sleep(1000); // 等待1秒
                
                // 重新轮询一次
                pollResult = bilibiliLoginService.pollQRCodeLogin(qrcodeKey);
                if (pollResult != null && pollResult.getIntValue("code") == 0) {
                    // 同样，pollResult就是data对象，不需要再次获取data字段
                    // data = pollResult.getJSONObject("data");
                    data = pollResult;
                    if (data != null) {
                        code = data.getIntValue("code");
                        message = data.getString("message");
                        log.info("重试后二维码状态 - Code: {}, Message: {}", code, message);
                    }
                }
            }
            
            // 如果是登录成功状态，保存登录信息
            if (code == 0) {
                log.info("二维码登录成功，准备保存登录信息");
                saveLoginInfo(data);
                log.info("登录信息保存完成");
            }
            
            // 直接返回Bilibili API的状态码和消息
            return new PollResult(code, message, data);
            
        } catch (Exception e) {
            log.error("轮询二维码状态失败: {}", e.getMessage(), e);
            // 发生异常时返回未扫码状态，继续轮询
            return new PollResult(86101, "未扫码", null);
        }
    }
    
    @Override
    public QRCodeLoginResult performQRCodeLogin() {
        try {
            log.info("开始执行二维码登录流程");
            
            // 1. 生成二维码
            JSONObject qrCodeInfo = generateQRCode();
            if (qrCodeInfo == null || qrCodeInfo.getIntValue("code") != 0) {
                return new QRCodeLoginResult(false, "生成二维码失败", null);
            }
            
            JSONObject qrData = qrCodeInfo.getJSONObject("data");
            String qrCodeUrl = qrData.getString("url");
            String qrcodeKey = qrData.getString("qrcode_key");
            
            log.info("二维码已生成，URL: {}", qrCodeUrl);
            
            // 2. 轮询扫码状态
            int maxAttempts = 30; // 最大轮询次数
            int attempt = 0;
            
            while (attempt < maxAttempts) {
                Thread.sleep(3000); // 每3秒轮询一次
                attempt++;
                
                // 修复：直接调用bilibiliLoginService.pollQRCodeLogin而不是pollQRCodeStatus
                JSONObject pollResult = bilibiliLoginService.pollQRCodeLogin(qrcodeKey);
                
                if (pollResult == null) {
                    log.info("等待用户扫码... (尝试 {}/{})", attempt, maxAttempts);
                    continue;
                }
                
                // 检查外层API调用是否成功
                int apiCode = pollResult.getIntValue("code");
                if (apiCode != 0) {
                    log.info("等待用户扫码... (尝试 {}/{})", attempt, maxAttempts);
                    continue;
                }
                
                // 注意：pollQRCodeLogin已经返回了data对象，不需要再次获取data字段
                // JSONObject data = pollResult.getJSONObject("data");
                JSONObject data = pollResult; // 正确的做法
                
                if (data == null) {
                    log.info("等待用户扫码... (尝试 {}/{})", attempt, maxAttempts);
                    continue;
                }
                
                int code = data.getIntValue("code");
                String message = data.getString("message");
                
                log.info("轮询结果 - Code: {}, Message: {}", code, message);
                
                switch (code) {
                    case 0: // 登录成功
                        log.info("二维码登录成功，准备保存登录信息");
                        // 保存登录信息到文件
                        saveLoginInfo(data);
                        log.info("登录信息保存完成");
                        String cookies = extractCookiesFromData(data);
                        log.info("二维码登录流程完成");
                        return new QRCodeLoginResult(true, "登录成功", cookies);
                        
                    case 86101: // 未扫码
                        log.info("等待用户扫码... (尝试 {}/{})", attempt, maxAttempts);
                        break;
                        
                    case 86090: // 已扫描未确认
                        log.info("用户已扫描，请在手机上确认... (尝试 {}/{})", attempt, maxAttempts);
                        break;
                        
                    case 86038: // 二维码已失效
                        log.warn("二维码已失效");
                        return new QRCodeLoginResult(false, "二维码已失效", null);
                        
                    default:
                        log.warn("未知状态: {}", message);
                        break;
                }
            }
            
            return new QRCodeLoginResult(false, "登录超时", null);
            
        } catch (Exception e) {
            log.error("二维码登录流程失败: {}", e.getMessage(), e);
            return new QRCodeLoginResult(false, "登录失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 从登录数据中提取Cookie信息
     * @param data 登录数据
     * @return Cookie字符串
     */
    private String extractCookiesFromData(JSONObject data) {
        if (data == null) {
            return null;
        }
        
        try {
            // 根据Bilibili API的实际响应结构提取Cookie
            // 打印data内容以便调试
            log.debug("登录数据内容: {}", data.toJSONString());
            
            // 尝试不同的方式提取cookie信息
            if (data.containsKey("cookie")) {
                return data.getString("cookie");
            } else if (data.containsKey("cookies")) {
                return data.getString("cookies");
            } else if (data.containsKey("data") && data.getObject("data", JSONObject.class) != null) {
                // 如果data字段本身又是一个JSONObject
                JSONObject innerData = data.getObject("data", JSONObject.class);
                if (innerData.containsKey("cookie")) {
                    return innerData.getString("cookie");
                } else if (innerData.containsKey("cookies")) {
                    return innerData.getString("cookies");
                }
            }
            
            // 如果以上方式都找不到cookie，返回整个data的字符串表示
            return data.toJSONString();
        } catch (Exception e) {
            log.warn("提取Cookie信息时出错: {}", e.getMessage());
            // 出错时返回整个data的字符串表示
            return data.toJSONString();
        }
    }
    
    /**
     * 保存登录信息到文件和数据库
     * @param loginData 登录数据
     */
    private void saveLoginInfo(JSONObject loginData) {
        try {
            log.info("开始保存登录信息");
            log.debug("准备保存登录信息，原始数据: {}", loginData.toJSONString());
            
            // 1. 保存到本地文件
            // 创建包含时间戳的登录信息
            JSONObject loginInfo = new JSONObject();
            loginInfo.put("loginTime", System.currentTimeMillis());
            loginInfo.put("data", loginData);
            
            // 写入文件
            try (FileWriter writer = new FileWriter(LOGIN_INFO_FILE)) {
                writer.write(loginInfo.toJSONString());
            }
            
            log.info("登录信息已保存到文件: {}", LOGIN_INFO_FILE);
            
            // 2. 保存到数据库
            try {
                LoginInfo dbLoginInfo = new LoginInfo();
                
                // 从登录数据中提取用户信息
                log.debug("尝试从登录数据中提取用户信息");
                
                // 提取用户ID和其他信息
                extractUserInfoFromLoginData(loginData, dbLoginInfo);
                
                // 设置Cookie信息
                dbLoginInfo.setCookieInfo(loginData.toJSONString());
                
                // 设置登录时间
                dbLoginInfo.setLoginTime(LocalDateTime.now());
                
                // 设置过期时间（假设24小时后过期）
                dbLoginInfo.setExpireTime(LocalDateTime.now().plusHours(24));
                
                // 检查是否提取到了用户ID
                if (dbLoginInfo.getUserId() != null) {
                    log.debug("准备保存到数据库的登录信息: userId={}, username={}, nickname={}", 
                             dbLoginInfo.getUserId(), dbLoginInfo.getUsername(), dbLoginInfo.getNickname());
                    // 保存到数据库
                    loginInfoService.saveOrUpdate(dbLoginInfo);
                    log.info("登录信息已保存到数据库，用户ID: {}", dbLoginInfo.getUserId());
                } else {
                    log.warn("未能提取到用户ID，跳过数据库保存。完整登录数据: {}", loginData.toJSONString());
                }
            } catch (Exception e) {
                log.error("保存登录信息到数据库失败: {}", e.getMessage(), e);
            }
            
        } catch (IOException e) {
            log.error("保存登录信息到文件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从登录数据中提取用户信息
     * @param loginData 登录数据
     * @param dbLoginInfo 数据库登录信息对象
     */
    private void extractUserInfoFromLoginData(JSONObject loginData, LoginInfo dbLoginInfo) {
        try {
            // 从URL中提取用户信息
            if (loginData.containsKey("url")) {
                String url = loginData.getString("url");
                log.debug("从URL中提取用户信息: {}", url);
                
                // 解析URL参数
                Map<String, String> params = parseUrlParams(url);
                
                // 提取用户ID
                if (params.containsKey("DedeUserID")) {
                    try {
                        Long userId = Long.parseLong(params.get("DedeUserID"));
                        dbLoginInfo.setUserId(userId);
                        log.debug("从URL提取到用户ID: {}", userId);
                    } catch (NumberFormatException e) {
                        log.warn("解析用户ID失败: {}", params.get("DedeUserID"));
                    }
                }
                
                // 提取其他信息（如果有的话）
                if (params.containsKey("SESSDATA")) {
                    // 可以将SESSDATA作为访问令牌存储
                    dbLoginInfo.setAccessToken(params.get("SESSDATA"));
                    log.debug("从URL提取到SESSDATA");
                }
                
                if (params.containsKey("bili_jct")) {
                    // 可以将bili_jct作为刷新令牌存储
                    dbLoginInfo.setRefreshToken(params.get("bili_jct"));
                    log.debug("从URL提取到bili_jct");
                }
            }
            
            // 如果URL中没有找到，再尝试其他方式
            if (dbLoginInfo.getUserId() == null) {
                // 尝试直接从loginData中提取用户信息（顶层）
                log.debug("尝试从顶层提取用户信息");
                if (loginData.containsKey("mid")) {
                    dbLoginInfo.setUserId(loginData.getLong("mid"));
                    log.debug("从顶层提取到用户ID: {}", loginData.getLong("mid"));
                }
                if (loginData.containsKey("uname")) {
                    dbLoginInfo.setUsername(loginData.getString("uname"));
                    log.debug("从顶层提取到用户名: {}", loginData.getString("uname"));
                }
                if (loginData.containsKey("nickname")) {
                    dbLoginInfo.setNickname(loginData.getString("nickname"));
                    log.debug("从顶层提取到昵称: {}", loginData.getString("nickname"));
                }
                if (loginData.containsKey("avatar")) {
                    dbLoginInfo.setAvatarUrl(loginData.getString("avatar"));
                    log.debug("从顶层提取到头像: {}", loginData.getString("avatar"));
                }
                
                // 如果顶层没有找到，再尝试从data字段中提取
                if (loginData.containsKey("data")) {
                    Object dataObj = loginData.get("data");
                    log.debug("data字段类型: {}, 内容: {}", dataObj.getClass().getSimpleName(), dataObj.toString());
                    
                    if (dataObj instanceof JSONObject) {
                        JSONObject data = (JSONObject) dataObj;
                        log.debug("data字段内容: {}", data.toJSONString());
                        if (dbLoginInfo.getUserId() == null && data.containsKey("mid")) {
                            dbLoginInfo.setUserId(data.getLong("mid"));
                            log.debug("从data字段提取到用户ID: {}", data.getLong("mid"));
                        }
                        if (dbLoginInfo.getUsername() == null && data.containsKey("uname")) {
                            dbLoginInfo.setUsername(data.getString("uname"));
                            log.debug("从data字段提取到用户名: {}", data.getString("uname"));
                        }
                        if (dbLoginInfo.getNickname() == null && data.containsKey("nickname")) {
                            dbLoginInfo.setNickname(data.getString("nickname"));
                            log.debug("从data字段提取到昵称: {}", data.getString("nickname"));
                        }
                        if (dbLoginInfo.getAvatarUrl() == null && data.containsKey("avatar")) {
                            dbLoginInfo.setAvatarUrl(data.getString("avatar"));
                            log.debug("从data字段提取到头像: {}", data.getString("avatar"));
                        }
                    } else {
                        log.debug("data字段不是JSONObject类型: {}", dataObj.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取用户信息时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 解析URL参数
     * @param url URL
     * @return 参数映射
     */
    private Map<String, String> parseUrlParams(String url) {
        Map<String, String> params = new HashMap<>();
        try {
            // 找到?后面的部分
            int queryStart = url.indexOf('?');
            if (queryStart > 0 && queryStart < url.length() - 1) {
                String query = url.substring(queryStart + 1);
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx > 0 && idx < pair.length() - 1) {
                        String key = pair.substring(0, idx);
                        String value = pair.substring(idx + 1);
                        // URL解码
                        try {
                            value = java.net.URLDecoder.decode(value, "UTF-8");
                        } catch (Exception e) {
                            // 解码失败时使用原始值
                        }
                        params.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析URL参数时出错: {}", e.getMessage());
        }
        return params;
    }
    
    /**
     * 从文件或数据库读取登录信息
     * @return 登录信息
     */
    public JSONObject getLoginInfo() {
        try {
            // 1. 优先从本地文件读取
            File file = new File(LOGIN_INFO_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(LOGIN_INFO_FILE)));
                return JSONObject.parseObject(content);
            }
            
            // 2. 如果本地文件不存在，尝试从数据库读取
            log.info("本地登录信息文件不存在，尝试从数据库读取");
            
            // 这里需要根据实际业务逻辑确定如何获取用户ID
            // 暂时返回null，实际应用中可能需要其他方式获取用户ID
            // 比如可以通过某种方式获取最近登录的用户ID
            
        } catch (Exception e) {
            log.error("读取登录信息失败: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * 检查是否已登录
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        JSONObject loginInfo = getLoginInfo();
        if (loginInfo == null) {
            return false;
        }
        
        // 检查登录是否过期（假设有效期为24小时）
        long loginTime = loginInfo.getLongValue("loginTime");
        long currentTime = System.currentTimeMillis();
        long expireTime = 24 * 60 * 60 * 1000; // 24小时
        
        return (currentTime - loginTime) < expireTime;
    }
    
    /**
     * 注销登录
     */
    public void logout() {
        try {
            File file = new File(LOGIN_INFO_FILE);
            if (file.exists()) {
                file.delete();
                log.info("已注销登录");
            }
        } catch (Exception e) {
            log.error("注销登录失败: {}", e.getMessage(), e);
        }
    }
}