package com.tbw.cut.controller;

import com.tbw.cut.service.FileScannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/file-scanner")
public class FileScannerController {
    
    @Autowired
    private FileScannerService fileScannerService;
    
    /**
     * 扫描指定路径下的文件和文件夹
     * @param path 要扫描的路径
     * @return 文件和文件夹列表
     */
    @GetMapping("/scan")
    public Result<List<Map<String, Object>>> scanPath(@RequestParam(required = false) String path) {
        try {
            List<Map<String, Object>> files = fileScannerService.scanPath(path);
            return Result.success(files);
        } catch (Exception e) {
            log.error("扫描路径失败: {}", path, e);
            return Result.error("扫描路径失败: " + e.getMessage());
        }
    }
    
    /**
     * 统一响应结果类
     */
    public static class Result<T> {
        private int code;
        private String message;
        private T data;
        
        public static <T> Result<T> success(T data) {
            Result<T> result = new Result<>();
            result.code = 0;
            result.message = "success";
            result.data = data;
            return result;
        }
        
        public static <T> Result<T> error(String message) {
            Result<T> result = new Result<>();
            result.code = -1;
            result.message = message;
            return result;
        }
        
        // Getters and setters
        public int getCode() {
            return code;
        }
        
        public void setCode(int code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public T getData() {
            return data;
        }
        
        public void setData(T data) {
            this.data = data;
        }
    }
}