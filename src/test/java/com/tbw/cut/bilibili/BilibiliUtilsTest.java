package com.tbw.cut.bilibili;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BilibiliUtilsTest {

    @Test
    void testExtractBvidFromUrl() {
        // 测试正常情况
        String url1 = "https://www.bilibili.com/video/BV1Xx411175n";
        assertEquals("BV1Xx411175n", BilibiliUtils.extractBvidFromUrl(url1));
        
        // 测试包含其他参数的URL
        String url2 = "https://www.bilibili.com/video/BV1Xx411175n?p=1";
        assertEquals("BV1Xx411175n", BilibiliUtils.extractBvidFromUrl(url2));
        
        // 测试无效URL
        String url3 = "https://www.bilibili.com/video/av123456";
        assertNull(BilibiliUtils.extractBvidFromUrl(url3));
        
        // 测试null输入
        assertNull(BilibiliUtils.extractBvidFromUrl(null));
        
        // 测试空字符串输入
        assertNull(BilibiliUtils.extractBvidFromUrl(""));
    }
    
    @Test
    void testExtractAidFromUrl() {
        // 测试正常情况
        String url1 = "https://www.bilibili.com/video/av123456";
        assertEquals("av123456", BilibiliUtils.extractAidFromUrl(url1));
        
        // 测试包含其他参数的URL
        String url2 = "https://www.bilibili.com/video/av123456?p=1";
        assertEquals("av123456", BilibiliUtils.extractAidFromUrl(url2));
        
        // 测试无效URL
        String url3 = "https://www.bilibili.com/video/BV1Xx411175n";
        assertNull(BilibiliUtils.extractAidFromUrl(url3));
        
        // 测试null输入
        assertNull(BilibiliUtils.extractAidFromUrl(null));
        
        // 测试空字符串输入
        assertNull(BilibiliUtils.extractAidFromUrl(""));
    }
    
    @Test
    void testExtractRoomIdFromUrl() {
        // 测试正常情况
        String url1 = "https://live.bilibili.com/123456";
        assertEquals("123456", BilibiliUtils.extractRoomIdFromUrl(url1));
        
        // 测试包含其他参数的URL
        String url2 = "https://live.bilibili.com/123456?visit_id=abc";
        assertEquals("123456", BilibiliUtils.extractRoomIdFromUrl(url2));
        
        // 测试无效URL
        String url3 = "https://www.bilibili.com/video/BV1Xx411175n";
        assertNull(BilibiliUtils.extractRoomIdFromUrl(url3));
        
        // 测试null输入
        assertNull(BilibiliUtils.extractRoomIdFromUrl(null));
        
        // 测试空字符串输入
        assertNull(BilibiliUtils.extractRoomIdFromUrl(""));
    }
    
    @Test
    void testFormatViewCount() {
        // 测试亿级
        assertEquals("1.2亿", BilibiliUtils.formatViewCount(123456789L));
        
        // 测试万级
        assertEquals("12.3万", BilibiliUtils.formatViewCount(123456L));
        
        // 测试千级及以下
        assertEquals("1234", BilibiliUtils.formatViewCount(1234L));
        
        // 测试null
        assertEquals("0", BilibiliUtils.formatViewCount(null));
    }
    
    @Test
    void testFormatDuration() {
        // 测试小时级
        assertEquals("01:02:03", BilibiliUtils.formatDuration(3723));
        
        // 测试分钟级
        assertEquals("10:30", BilibiliUtils.formatDuration(630));
        
        // 测试秒级
        assertEquals("00:30", BilibiliUtils.formatDuration(30));
    }
}