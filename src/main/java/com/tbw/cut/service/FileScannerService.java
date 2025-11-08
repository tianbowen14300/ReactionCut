package com.tbw.cut.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface FileScannerService {
    
    /**
     * 扫描指定路径下的文件和文件夹
     * @param path 要扫描的路径
     * @return 文件和文件夹列表
     * @throws IOException IO异常
     */
    List<Map<String, Object>> scanPath(String path) throws IOException;
}