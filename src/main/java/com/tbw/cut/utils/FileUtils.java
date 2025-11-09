package com.tbw.cut.utils;

import java.io.File;

public class FileUtils {

    /**
     * 从完整路径中获取文件名（不含后缀），性能最优。
     * 依赖纯粹的 String 查找和截取操作，避免对象创建。
     *
     * @param fullPath 完整文件路径
     * @return 不含后缀的文件名
     */
    public static String getBaseName(String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            return fullPath;
        }

        // 1. 找到文件名开始的位置（最后一个路径分隔符）
        // 兼容 Unix/Linux (/) 和 Windows (\)
        int sepIndex = fullPath.lastIndexOf(File.separator);

        // 如果路径分隔符是反斜杠，需要检查
        if (sepIndex == -1) {
            sepIndex = fullPath.lastIndexOf('/'); // 重新检查正斜杠
        }

        // 文件名从分隔符后一位开始 (如果找不到分隔符，则从 0 开始)
        String fileName = (sepIndex == -1) ? fullPath : fullPath.substring(sepIndex + 1);

        // 2. 在文件名中找到最后一个点（.）的位置
        int dotIndex = fileName.lastIndexOf('.');

        // 3. 截取（处理无后缀或点在开头的情况，如 .gitignore）
        if (dotIndex <= 0) {
            return fileName; // 没有点或点在开头，返回完整文件名
        }

        return fileName.substring(0, dotIndex);
    }
}
