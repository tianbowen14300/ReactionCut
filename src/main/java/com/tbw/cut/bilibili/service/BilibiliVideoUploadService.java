package com.tbw.cut.bilibili.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Bilibili视频上传服务接口
 */
public interface BilibiliVideoUploadService {
    
    /**
     * 获取上传元数据 (预上传)
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @return 上传元数据
     */
    JSONObject preUploadVideo(String fileName, long fileSize);
    
    /**
     * 上传视频元数据
     * @param preUploadData 预上传返回的数据
     * @param fileSize 文件大小
     * @return 上传ID等信息
     */
    JSONObject postVideoMeta(JSONObject preUploadData, long fileSize);
    
    /**
     * 分片上传视频文件
     * @param preUploadData 预上传返回的数据
     * @param postVideoMeta 上传元数据返回的数据
     * @param videoFile 视频文件
     * @return 分块数量
     */
    int uploadVideo(JSONObject preUploadData, JSONObject postVideoMeta, File videoFile);
    
    /**
     * 结束上传视频文件
     * @param preUploadData 预上传返回的数据
     * @param postVideoMeta 上传元数据返回的数据
     * @param chunks 分块数量
     * @return 上传结果
     */
    JSONObject endUpload(JSONObject preUploadData, JSONObject postVideoMeta, int chunks);
    
    /**
     * 上传视频封面
     * @param coverImage 封面图片
     * @return 封面URL
     */
    String uploadCover(MultipartFile coverImage);
    
    /**
     * 提交视频投稿
     * @param submitData 投稿数据
     * @return 投稿结果，包含aid和bvid
     */
    JSONObject submitVideo(JSONObject submitData);
}