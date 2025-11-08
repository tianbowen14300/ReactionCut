package com.tbw.cut.bilibili;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.utils.WbiSignUtils;
import com.tbw.cut.entity.LoginInfo;
import com.tbw.cut.service.LoginInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class BilibiliApiClient {
    
    private static final String LOGIN_INFO_FILE = System.getProperty("user.dir") + "/bilibili_login_info.json";
    private RestTemplate restTemplate;
    
    @Autowired
    private LoginInfoService loginInfoService;
    
    @Autowired
    private WbiSignUtils wbiSignUtils;
    
    @Value("${bilibili.api.base-url:https://api.bilibili.com}")
    private String baseUrl;
    
    @Value("${bilibili.api.timeout:5000}")
    private int timeout;
    
    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 通用GET请求方法
     * @param url 完整URL
     * @param params 请求参数
     * @return 响应字符串
     */
    public String get(String url, Map<String, String> params) {
        return get(url, params, null);
    }
    
    /**
     * 带请求头的GET请求方法
     * @param url 完整URL
     * @param params 请求参数
     * @param headers 请求头
     * @return 响应字符串
     */
    public String get(String url, Map<String, String> params, Map<String, String> headers) {
        try {
            String fullUrl = buildUrl(url, params);
            
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach((key, value) -> httpHeaders.set(key, value));
            }
            
            // 设置默认User-Agent
            if (!httpHeaders.containsKey("User-Agent")) {
                httpHeaders.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            }
            
            // 添加认证信息
            addAuthHeaders(httpHeaders);
            
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            
            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, HttpMethod.GET, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Bilibili API GET请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("Bilibili API请求失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 带WBI签名的GET请求方法
     * @param url 完整URL
     * @param params 请求参数
     * @return 响应字符串
     */
    public String getWithWbiSign(String url, Map<String, String> params) {
        return getWithWbiSign(url, params, null);
    }
    
    /**
     * 带WBI签名和请求头的GET请求方法
     * @param url 完整URL
     * @param params 请求参数
     * @param headers 请求头
     * @return 响应字符串
     */
    public String getWithWbiSign(String url, Map<String, String> params, Map<String, String> headers) {
        try {
            // 对参数进行WBI签名
            String signedParamsStr = wbiSignUtils.signParams(params);
            
            // 构建完整URL
            String fullUrl = url + "?" + signedParamsStr;
            
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach((key, value) -> httpHeaders.set(key, value));
            }
            
            // 设置默认User-Agent
            if (!httpHeaders.containsKey("User-Agent")) {
                httpHeaders.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            }
            
            // 添加认证信息
            addAuthHeaders(httpHeaders);
            
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            
            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, HttpMethod.GET, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Bilibili API GET请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("Bilibili API请求失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 通用POST请求方法
     * @param url 完整URL
     * @param params 请求参数
     * @param body 请求体
     * @return 响应字符串
     */
    public String post(String url, Map<String, String> params, Object body) {
        return post(url, params, body, null);
    }
    
    /**
     * 带请求头的POST请求方法
     * @param url 完整URL
     * @param params 请求参数
     * @param body 请求体
     * @param headers 请求头
     * @return 响应字符串
     */
    public String post(String url, Map<String, String> params, Object body, Map<String, String> headers) {
        try {
            String fullUrl = buildUrl(url, params);
            
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            if (headers != null) {
                headers.forEach((key, value) -> httpHeaders.set(key, value));
            }
            
            // 设置默认User-Agent
            if (!httpHeaders.containsKey("User-Agent")) {
                httpHeaders.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            }
            
            // 添加认证信息
            addAuthHeaders(httpHeaders);
            
            HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);
            
            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, HttpMethod.POST, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Bilibili API POST请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("Bilibili API请求失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建完整URL
     * @param url 基础URL
     * @param params 请求参数
     * @return 完整URL
     */
    private String buildUrl(String url, Map<String, String> params) {
        StringBuilder fullUrl = new StringBuilder(url);
        
        if (params != null && !params.isEmpty()) {
            // Check if URL already contains query parameters
            boolean hasQuery = url.contains("?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (hasQuery) {
                    fullUrl.append("&");
                } else {
                    fullUrl.append("?");
                    hasQuery = true;
                }
                fullUrl.append(entry.getKey()).append("=").append(entry.getValue());
                log.debug("添加参数: {}={}", entry.getKey(), entry.getValue());
            }
        }
        
        String result = fullUrl.toString();
        log.debug("构建的完整URL: {}", result);
        return result;
    }
    
    /**
     * 添加认证头信息
     * @param headers 请求头
     */
    private void addAuthHeaders(HttpHeaders headers) {
        try {
            // 1. 优先从本地文件读取认证信息
            File file = new File(LOGIN_INFO_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(LOGIN_INFO_FILE)));
                com.alibaba.fastjson.JSONObject loginInfo = com.alibaba.fastjson.JSONObject.parseObject(content);
                
                if (loginInfo != null && loginInfo.containsKey("data")) {
                    com.alibaba.fastjson.JSONObject data = loginInfo.getJSONObject("data");
                    // 从登录数据中提取并添加Cookie头
                    if (data.containsKey("cookie")) {
                        headers.add("Cookie", data.getString("cookie"));
                        log.debug("从本地文件添加Cookie头: {}", data.getString("cookie"));
                    } else if (data.containsKey("cookies")) {
                        headers.add("Cookie", data.getString("cookies"));
                        log.debug("从本地文件添加Cookies头: {}", data.getString("cookies"));
                    } else if (data.containsKey("url")) {
                        // 从URL中提取Cookie信息
                        String cookie = extractCookieFromUrl(data.getString("url"));
                        if (cookie != null) {
                            headers.add("Cookie", cookie);
                            log.debug("从本地文件URL中提取并添加Cookie头: {}", cookie);
                        } else {
                            log.debug("本地文件中未找到有效的Cookie信息");
                        }
                    } else {
                        log.debug("本地文件中未找到Cookie信息");
                    }
                } else {
                    log.debug("本地文件登录信息格式不正确或缺少data字段");
                }
                return;
            }
            
            // 2. 如果本地文件不存在，尝试从数据库读取
            log.debug("本地登录信息文件不存在，尝试从数据库读取");
            
            // 这里需要根据实际业务逻辑确定如何获取用户ID
            // 暂时使用一个默认的用户ID，实际应用中可能需要其他方式获取用户ID
            // 比如可以通过某种方式获取最近登录的用户ID
            Long userId = 1L; // 默认用户ID，实际应用中需要动态获取
            LoginInfo dbLoginInfo = loginInfoService.getByUserId(userId);
            
            if (dbLoginInfo != null) {
                LoginInfo loginInfo = dbLoginInfo;
                // 从数据库中的Cookie信息中提取并添加Cookie头
                if (loginInfo.getCookieInfo() != null) {
                    com.alibaba.fastjson.JSONObject cookieData = com.alibaba.fastjson.JSONObject.parseObject(loginInfo.getCookieInfo());
                    if (cookieData.containsKey("data")) {
                        com.alibaba.fastjson.JSONObject data = cookieData.getJSONObject("data");
                        if (data.containsKey("cookie")) {
                            headers.add("Cookie", data.getString("cookie"));
                            log.debug("从数据库添加Cookie头: {}", data.getString("cookie"));
                        } else if (data.containsKey("cookies")) {
                            headers.add("Cookie", data.getString("cookies"));
                            log.debug("从数据库添加Cookies头: {}", data.getString("cookies"));
                        } else if (data.containsKey("url")) {
                            // 从URL中提取Cookie信息
                            String cookie = extractCookieFromUrl(data.getString("url"));
                            if (cookie != null) {
                                headers.add("Cookie", cookie);
                                log.debug("从数据库URL中提取并添加Cookie头: {}", cookie);
                            }
                        }
                    }
                }
                
                // 同时生成本地文件
                generateLoginInfoFile(loginInfo);
                return;
            }
            
            // 3. 如果数据库也不存在，提示需要登录
            log.warn("本地文件和数据库中都不存在登录信息，请先登录");
            
        } catch (Exception e) {
            log.warn("读取认证信息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从跨域URL中提取Cookie信息
     * @param url 跨域URL
     * @return Cookie字符串
     */
    private String extractCookieFromUrl(String url) {
        try {
            if (url == null || url.isEmpty()) {
                return null;
            }
            
            // 解析URL中的查询参数
            String[] parts = url.split("\\?");
            if (parts.length < 2) {
                return null;
            }
            
            String queryString = parts[1];
            String[] params = queryString.split("&");
            
            StringBuilder cookieBuilder = new StringBuilder();
            boolean hasSessdata = false;
            boolean hasBiliJct = false;
            boolean hasDedeUserID = false;
            
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    
                    switch (key) {
                        case "SESSDATA":
                            cookieBuilder.append("SESSDATA=").append(value).append("; ");
                            hasSessdata = true;
                            break;
                        case "bili_jct":
                            cookieBuilder.append("bili_jct=").append(value).append("; ");
                            hasBiliJct = true;
                            break;
                        case "DedeUserID":
                            cookieBuilder.append("DedeUserID=").append(value).append("; ");
                            hasDedeUserID = true;
                            break;
                    }
                }
            }
            
            // 只有当所有必要信息都存在时才返回Cookie
            if (hasSessdata && hasBiliJct && hasDedeUserID) {
                // 移除末尾的分号和空格
                String cookie = cookieBuilder.toString().trim();
                if (cookie.endsWith(";")) {
                    cookie = cookie.substring(0, cookie.length() - 1);
                }
                return cookie;
            }
        } catch (Exception e) {
            log.warn("从URL提取Cookie信息失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 从登录信息中提取Cookie
     * @return Cookie字符串
     */
    public String extractCookieFromLoginInfo() {
        try {
            // 1. 优先从本地文件读取认证信息
            File file = new File(LOGIN_INFO_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(LOGIN_INFO_FILE)));
                JSONObject loginInfo = JSONObject.parseObject(content);
                
                // 从登录信息中提取Cookie
                if (loginInfo.containsKey("data")) {
                    JSONObject data = loginInfo.getJSONObject("data");
                    if (data.containsKey("cookie")) {
                        return data.getString("cookie");
                    } else if (data.containsKey("cookies")) {
                        return data.getString("cookies");
                    } else if (data.containsKey("url")) {
                        // 从URL中提取Cookie信息
                        return extractCookieFromUrl(data.getString("url"));
                    }
                }
                return null;
            }
            
            // 2. 如果本地文件不存在，尝试从数据库读取
            log.debug("本地登录信息文件不存在，尝试从数据库读取");
            
            // 这里需要根据实际业务逻辑确定如何获取用户ID
            // 暂时使用一个默认的用户ID，实际应用中可能需要其他方式获取用户ID
            // 比如可以通过某种方式获取最近登录的用户ID
            Long userId = 1L; // 默认用户ID，实际应用中需要动态获取
            LoginInfo dbLoginInfo = loginInfoService.getByUserId(userId);
            
            if (dbLoginInfo != null) {
                LoginInfo loginInfo = dbLoginInfo;
                // 从数据库中的Cookie信息中提取并添加Cookie头
                if (loginInfo.getCookieInfo() != null) {
                    JSONObject cookieData = JSONObject.parseObject(loginInfo.getCookieInfo());
                    if (cookieData.containsKey("data")) {
                        JSONObject data = cookieData.getJSONObject("data");
                        if (data.containsKey("cookie")) {
                            // 同时生成本地文件
                            generateLoginInfoFile(loginInfo);
                            return data.getString("cookie");
                        } else if (data.containsKey("cookies")) {
                            // 同时生成本地文件
                            generateLoginInfoFile(loginInfo);
                            return data.getString("cookies");
                        } else if (data.containsKey("url")) {
                            // 从URL中提取Cookie信息
                            String cookie = extractCookieFromUrl(data.getString("url"));
                            if (cookie != null) {
                                // 同时生成本地文件
                                generateLoginInfoFile(loginInfo);
                                return cookie;
                            }
                        }
                    }
                }
                
                // 同时生成本地文件
                generateLoginInfoFile(loginInfo);
                return null;
            }
            
            // 3. 如果数据库也不存在，提示需要登录
            log.warn("本地文件和数据库中都不存在登录信息，请先登录");
            
        } catch (Exception e) {
            log.warn("读取认证信息失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 从登录信息中提取CSRF Token (bili_jct)
     * @return CSRF Token字符串
     */
    public String extractCsrfTokenFromLoginInfo() {
        try {
            // 1. 优先从本地文件读取认证信息
            File file = new File(LOGIN_INFO_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(LOGIN_INFO_FILE)));
                JSONObject loginInfo = JSONObject.parseObject(content);
                
                // 从登录信息中提取CSRF Token
                if (loginInfo.containsKey("data")) {
                    JSONObject data = loginInfo.getJSONObject("data");
                    if (data.containsKey("cookie")) {
                        return extractCsrfFromCookie(data.getString("cookie"));
                    } else if (data.containsKey("cookies")) {
                        return extractCsrfFromCookie(data.getString("cookies"));
                    } else if (data.containsKey("url")) {
                        // 从URL中提取Cookie信息，然后提取CSRF Token
                        String cookie = extractCookieFromUrl(data.getString("url"));
                        return extractCsrfFromCookie(cookie);
                    }
                }
                return null;
            }
            
            // 2. 如果本地文件不存在，尝试从数据库读取
            log.debug("本地登录信息文件不存在，尝试从数据库读取");
            
            // 这里需要根据实际业务逻辑确定如何获取用户ID
            Long userId = 1L; // 默认用户ID，实际应用中需要动态获取
            LoginInfo dbLoginInfo = loginInfoService.getByUserId(userId);
            
            if (dbLoginInfo != null) {
                LoginInfo loginInfo = dbLoginInfo;
                // 从数据库中的Cookie信息中提取CSRF Token
                if (loginInfo.getCookieInfo() != null) {
                    JSONObject cookieData = JSONObject.parseObject(loginInfo.getCookieInfo());
                    if (cookieData.containsKey("data")) {
                        JSONObject data = cookieData.getJSONObject("data");
                        if (data.containsKey("cookie")) {
                            return extractCsrfFromCookie(data.getString("cookie"));
                        } else if (data.containsKey("cookies")) {
                            return extractCsrfFromCookie(data.getString("cookies"));
                        } else if (data.containsKey("url")) {
                            // 从URL中提取Cookie信息，然后提取CSRF Token
                            String cookie = extractCookieFromUrl(data.getString("url"));
                            if (cookie != null) {
                                // 同时生成本地文件
                                generateLoginInfoFile(loginInfo);
                                return extractCsrfFromCookie(cookie);
                            }
                        }
                    }
                }
                
                // 同时生成本地文件
                generateLoginInfoFile(loginInfo);
                return null;
            }
            
            // 3. 如果数据库也不存在，提示需要登录
            log.warn("本地文件和数据库中都不存在登录信息，请先登录");
            
        } catch (Exception e) {
            log.warn("读取认证信息失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 从Cookie字符串中提取CSRF Token (bili_jct)
     * @param cookie Cookie字符串
     * @return CSRF Token字符串
     */
    private String extractCsrfFromCookie(String cookie) {
        if (cookie == null || cookie.isEmpty()) {
            return null;
        }
        
        // 按分号分割Cookie
        String[] cookies = cookie.split(";");
        for (String c : cookies) {
            c = c.trim();
            if (c.startsWith("bili_jct=")) {
                return c.substring("bili_jct=".length());
            }
        }
        return null;
    }
    
    /**
     * 根据数据库登录信息生成本地文件
     * @param loginInfo 数据库登录信息
     */
    private void generateLoginInfoFile(LoginInfo loginInfo) {
        try {
            if (loginInfo.getCookieInfo() != null) {
                // 直接将数据库中的Cookie信息写入本地文件
                try (FileWriter writer = new FileWriter(LOGIN_INFO_FILE)) {
                    writer.write(loginInfo.getCookieInfo());
                }
                log.info("根据数据库信息生成本地登录文件: {}", LOGIN_INFO_FILE);
            }
        } catch (IOException e) {
            log.error("生成本地登录文件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 设置自定义RestTemplate
     * @param restTemplate RestTemplate实例
     */
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}