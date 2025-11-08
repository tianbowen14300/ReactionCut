package com.tbw.cut.bilibili.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * Bilibili多P视频上传服务接口
 * 参考biliup-rs的设计实现
 */
public interface BilibiliMultiPartUploadService {

    /**
     * 上传多个视频文件（多P上传）
     *
     * @param videoFiles 视频文件列表
     * @return 上传后的视频信息列表
     */
    List<JSONObject> uploadVideos(List<File> videoFiles);

    /**
     * 上传视频封面
     *
     * @param coverImage 封面图片
     * @return 封面URL
     */
    String uploadCover(MultipartFile coverImage);
}