package com.tbw.cut.service.impl;

import com.tbw.cut.service.FileScannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class FileScannerServiceImpl implements FileScannerService {
    
    @Value("${ffmpeg.video-storage-dir:/Users/tbw/Reaction}")
    private String basePath;
    
    @Override
    public List<Map<String, Object>> scanPath(String path) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 如果path为空或null，使用basePath
        if (path == null || path.isEmpty()) {
            path = basePath;
        }
        
        Path scanPath = Paths.get(path);
        
        // 检查路径是否存在
        if (!Files.exists(scanPath)) {
            throw new IOException("路径不存在: " + path);
        }
        
        // 检查是否为目录
        if (!Files.isDirectory(scanPath)) {
            throw new IOException("路径不是目录: " + path);
        }
        
        // 读取目录内容
        File directory = scanPath.toFile();
        File[] files = directory.listFiles();
        
        if (files != null) {
            // 按名称排序
            Arrays.sort(files, Comparator.comparing(File::getName));
            
            for (File file : files) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", file.getName());
                fileInfo.put("path", file.getAbsolutePath());
                fileInfo.put("isDirectory", file.isDirectory());
                fileInfo.put("size", file.isDirectory() ? 0 : file.length());
                fileInfo.put("lastModified", file.lastModified());
                
                result.add(fileInfo);
            }
        }
        
        return result;
    }
}