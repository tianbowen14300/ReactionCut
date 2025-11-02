package com.tbw.cut.bilibili;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BilibiliUtils {
    
    // BV号正则表达式
    private static final Pattern BV_PATTERN = Pattern.compile("BV([0-9a-zA-Z]+)");
    
    // AV号正则表达式
    private static final Pattern AV_PATTERN = Pattern.compile("av([0-9]+)");
    
    // 房间号正则表达式
    private static final Pattern ROOM_PATTERN = Pattern.compile("live\\.bilibili\\.com/([0-9]+)");
    
    /**
     * 从URL中提取BV号
     * @param url URL
     * @return BV号，如果未找到则返回null
     */
    public static String extractBvidFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        Matcher matcher = BV_PATTERN.matcher(url);
        if (matcher.find()) {
            return "BV" + matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 从URL中提取AV号
     * @param url URL
     * @return AV号，如果未找到则返回null
     */
    public static String extractAidFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        Matcher matcher = AV_PATTERN.matcher(url);
        if (matcher.find()) {
            return "av" + matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 从URL中提取房间号
     * @param url URL
     * @return 房间号，如果未找到则返回null
     */
    public static String extractRoomIdFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        Matcher matcher = ROOM_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 将AV号转换为BV号
     * @param aid AV号
     * @return BV号
     */
    public static String avToBv(String aid) {
        // 这是一个简化的实现，实际转换算法更复杂
        // 在实际应用中，您可能需要实现完整的AV到BV转换算法
        return "BV" + aid;
    }
    
    /**
     * 将BV号转换为AV号
     * @param bvid BV号
     * @return AV号
     */
    public static String bvToAv(String bvid) {
        // 这是一个简化的实现，实际转换算法更复杂
        // 在实际应用中，您可能需要实现完整的BV到AV转换算法
        return "av" + bvid.substring(2);
    }
    
    /**
     * 格式化播放量数字
     * @param viewCount 播放量
     * @return 格式化后的字符串
     */
    public static String formatViewCount(Long viewCount) {
        if (viewCount == null) {
            return "0";
        }
        
        if (viewCount >= 100000000) {
            return String.format("%.1f亿", viewCount / 100000000.0);
        } else if (viewCount >= 10000) {
            return String.format("%.1f万", viewCount / 10000.0);
        } else {
            return String.valueOf(viewCount);
        }
    }
    
    /**
     * 格式化时间
     * @param seconds 秒数
     * @return 格式化后的时间字符串 (HH:MM:SS)
     */
    public static String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }
}