package com.tbw.cut.utils;

public class FFmpegUtilTestMain {
    public static void main(String[] args) {
        FFmpegUtil ffmpegUtil = new FFmpegUtil();
        
        // 设置必要的字段
        try {
            // 使用反射设置私有字段
            java.lang.reflect.Field ffmpegPathField = FFmpegUtil.class.getDeclaredField("ffmpegPath");
            ffmpegPathField.setAccessible(true);
            ffmpegPathField.set(ffmpegUtil, "ffmpeg");
            
            java.lang.reflect.Field videoStorageDirField = FFmpegUtil.class.getDeclaredField("videoStorageDir");
            videoStorageDirField.setAccessible(true);
            videoStorageDirField.set(ffmpegUtil, "./videos");
            
            java.lang.reflect.Field tempDirField = FFmpegUtil.class.getDeclaredField("tempDir");
            tempDirField.setAccessible(true);
            tempDirField.set(ffmpegUtil, "./temp");
            
            // 测试下载视频
            String videoUrl = "https://upos-sz-estgoss.bilivideo.com/upgcxcode/64/53/33571145364/33571145364-1-30116.m4s?e=ig8euxZM2rNcNbdlhoNvNC8BqJIzNbfqXBvEqxTEto8BTrNvN0GvT90W5JZMkX_YN0MvXg8gNEV4NC8xNEV4N03eN0B5tZlqNxTEto8BTrNvNeZVuJ10Kj_g2UB02J0mN0B5tZlqNCNEto8BTrNvNC7MTX502C8f2jmMQJ6mqF2fka1mqx6gqj0eN0B599M=&trid=0aaa45240a304fd2b8ebbc974e14534u&os=estgoss&oi=666239813&platform=pc&uipk=5&gen=playurlv3&og=ali&mid=82679456&deadline=1761999694&nbs=1&upsig=f1767a938b76f6a868622c8f87b3c935&uparams=e,trid,os,oi,platform,uipk,gen,og,mid,deadline,nbs&bvc=vod&nettype=0&bw=2053886&f=u_0_0&agrr=1&buvid=&build=0&dl=0&orderid=0,3";
            String outputFileName = "test_video.mp4";
            
            String result = ffmpegUtil.downloadVideo(videoUrl, outputFileName);
            
            System.out.println("下载结果: " + result);
            
            if (result != null) {
                System.out.println("视频下载成功: " + result);
            } else {
                System.out.println("视频下载失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}