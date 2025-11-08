package com.tbw.cut.bilibili.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import com.tbw.cut.bilibili.service.BilibiliVideoUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

/**
 * Bilibili视频上传服务实现
 */
@Slf4j
@Service
public class BilibiliVideoUploadServiceImpl implements BilibiliVideoUploadService {
    
    @Autowired
    private BilibiliApiClient apiClient;
    
    @Value("${bilibili.member.base-url:https://member.bilibili.com}")
    private String memberBaseUrl;
    
    @Override
    public JSONObject preUploadVideo(String fileName, long fileSize) {
        try {
            String url = memberBaseUrl + "/preupload";
            StringJoiner params = new StringJoiner("&");
            params.add("name=" + URLEncoder.encode(fileName, "UTF-8"));
            params.add("r=upos");
            params.add("profile=ugcfx/bup");
            params.add("size=" + fileSize);
            
            String fullUrl = url + "?" + params.toString();
            log.info("预上传视频: {}", fullUrl);
            
            // 这里需要直接调用API，因为需要特殊的认证方式
            String response = callPreUploadApi(fullUrl);
            return JSONObject.parseObject(response);
        } catch (Exception e) {
            log.error("预上传视频失败: {}", e.getMessage(), e);
            throw new RuntimeException("预上传视频失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject postVideoMeta(JSONObject preUploadData, long fileSize) {
        try {
            String schemeAndHost = "https:" + preUploadData.getString("endpoint");
            String path = preUploadData.getString("upos_uri").replaceFirst("upos:/", "");
            String url = schemeAndHost + path;
            
            StringJoiner params = new StringJoiner("&");
            params.add("uploads="); // 留空是必须的
            params.add("output=json");
            params.add("profile=ugcfx/bup");
            params.add("filesize=" + fileSize);
            params.add("partsize=" + preUploadData.getLong("chunk_size"));
            params.add("biz_id=" + preUploadData.getLong("biz_id"));
            
            String fullUrl = url + "?" + params.toString();
            log.info("上传视频元数据: {}", fullUrl);
            
            String auth = preUploadData.getString("auth");
            String response = callPostVideoMetaApi(fullUrl, auth);
            return JSONObject.parseObject(response);
        } catch (Exception e) {
            log.error("上传视频元数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传视频元数据失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public int uploadVideo(JSONObject preUploadData, JSONObject postVideoMeta, File videoFile) {
        try {
            long startTs = System.currentTimeMillis() - 1;
            String schemeAndHost = "https:" + preUploadData.getString("endpoint");
            String path = preUploadData.getString("upos_uri").replaceFirst("upos:/", "");
            String urlBase = schemeAndHost + path;
            
            long length = videoFile.length();
            int chunkSize = preUploadData.getIntValue("chunk_size");
            byte[] buffer = new byte[chunkSize];
            int chunks = (int) Math.ceil((double) length / chunkSize);
            
            try (InputStream in = new FileInputStream(videoFile)) {
                String uploadId = postVideoMeta.getString("upload_id");
                
                for (int chunk = 0; chunk < chunks; chunk++) {
                    log.info("上传进度: {}/{} (速度: {} bytes/s)", chunk + 1, chunks, 
                            (chunk * chunkSize) / (System.currentTimeMillis() - startTs));
                    
                    int size = in.read(buffer, 0, chunkSize);
                    if (size == -1) {
                        break;
                    }
                    
                    StringJoiner params = new StringJoiner("&");
                    params.add("partNumber=" + (chunk + 1));
                    params.add("uploadId=" + uploadId);
                    params.add("chunk=" + chunk);
                    params.add("chunks=" + chunks);
                    params.add("size=" + size);
                    params.add("start=" + (chunk * chunkSize));
                    params.add("end=" + (chunk * chunkSize + size));
                    params.add("total=" + length);
                    
                    String fullUrl = urlBase + "?" + params.toString();
                    String auth = preUploadData.getString("auth");
                    
                    String response = callUploadChunkApi(fullUrl, auth, buffer, size);
                    if (!"MULTIPART_PUT_SUCCESS".equals(response.trim())) {
                        throw new RuntimeException("上传分块失败: " + response);
                    }
                }
            }
            
            return chunks;
        } catch (Exception e) {
            log.error("上传视频文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传视频文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject endUpload(JSONObject preUploadData, JSONObject postVideoMeta, int chunks) {
        try {
            String schemeAndHost = "https:" + preUploadData.getString("endpoint");
            String path = preUploadData.getString("upos_uri").replaceFirst("upos:/", "");
            String url = schemeAndHost + path;
            
            StringJoiner params = new StringJoiner("&");
            params.add("output=json");
            params.add("name=" + URLEncoder.encode(preUploadData.getString("filename"), "UTF-8"));
            params.add("profile=ugcfx/bup");
            params.add("uploadId=" + postVideoMeta.getString("upload_id"));
            params.add("biz_id=" + preUploadData.getLong("biz_id"));
            
            String fullUrl = url + "?" + params.toString();
            String auth = preUploadData.getString("auth");
            
            // 构建parts数组
            JSONObject body = new JSONObject();
            com.alibaba.fastjson.JSONArray parts = new com.alibaba.fastjson.JSONArray();
            for (int i = 1; i <= chunks; i++) {
                JSONObject part = new JSONObject();
                part.put("partNumber", i);
                part.put("eTag", "etag");
                parts.add(part);
            }
            body.put("parts", parts);
            
            String response = callEndUploadApi(fullUrl, auth, body.toJSONString());
            return JSONObject.parseObject(response);
        } catch (Exception e) {
            log.error("结束上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("结束上传失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String uploadCover(MultipartFile coverImage) {
        try {
            String url = memberBaseUrl + "/x/vu/web/cover/up";
            
            // 将图片文件转换为base64编码
            byte[] bytes = coverImage.getBytes();
            String base64Image = java.util.Base64.getEncoder().encodeToString(bytes);
            String imageData = "data:image/jpeg;base64," + base64Image;
            
            // 构建请求参数
            JSONObject params = new JSONObject();
            params.put("cover", imageData);
            params.put("csrf", apiClient.extractCsrfTokenFromLoginInfo());
            
            // 发送请求
            String response = callUploadCoverApi(url, params.toJSONString());
            JSONObject result = JSONObject.parseObject(response);
            
            if (result.getIntValue("code") == 0) {
                return result.getJSONObject("data").getString("url");
            } else {
                log.error("上传封面失败: {}", result.toJSONString());
                throw new RuntimeException("上传封面失败: " + result.getString("message"));
            }
        } catch (Exception e) {
            log.error("上传封面失败: {}", e.getMessage(), e);
            throw new RuntimeException("上传封面失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public JSONObject submitVideo(JSONObject submitData) {
        try {
            String url = memberBaseUrl + "/x/vu/web/add/v3";
            String csrf = apiClient.extractCsrfTokenFromLoginInfo();
            
            String fullUrl = url + "?csrf=" + csrf;
            log.info("提交视频投稿: {}", fullUrl);
            
            // 发送投稿请求
            String response = callSubmitVideoApi(fullUrl, submitData.toJSONString());
            return JSONObject.parseObject(response);
        } catch (Exception e) {
            log.error("提交视频投稿失败: {}", e.getMessage(), e);
            throw new RuntimeException("提交视频投稿失败: " + e.getMessage(), e);
        }
    }
    
    // 以下为辅助方法，用于直接调用HTTP API
    
    private String callPreUploadApi(String url) throws IOException {
        HttpURLConnection conn = createConnection(url, "GET");
        return readResponse(conn);
    }
    
    private String callPostVideoMetaApi(String url, String auth) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("X-Upos-Auth", auth);
        return readResponse(conn);
    }
    
    private String callUploadChunkApi(String url, String auth, byte[] data, int size) throws IOException {
        HttpURLConnection conn = createConnection(url, "PUT");
        conn.setRequestProperty("X-Upos-Auth", auth);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setDoOutput(true);
        
        try (OutputStream out = conn.getOutputStream()) {
            out.write(data, 0, size);
        }
        
        return readResponse(conn);
    }
    
    private String callEndUploadApi(String url, String auth, String body) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("X-Upos-Auth", auth);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        return readResponse(conn);
    }
    
    private HttpURLConnection createConnection(String url, String method) throws IOException {
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) new URI(url).toURL().openConnection();
        } catch (Exception e) {
            throw new IOException(e);
        }
        conn.setRequestMethod(method);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        // 添加认证信息
        String cookie = apiClient.extractCookieFromLoginInfo();
        if (cookie != null && !cookie.isEmpty()) {
            conn.setRequestProperty("Cookie", cookie);
        }
        
        return conn;
    }
    
    private String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    private String callUploadCoverApi(String url, String body) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        return readResponse(conn);
    }
    
    private String callSubmitVideoApi(String url, String body) throws IOException {
        HttpURLConnection conn = createConnection(url, "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        return readResponse(conn);
    }
}