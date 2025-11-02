package com.tbw.cut.controller;

import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.service.BilibiliVideoService;
import com.tbw.cut.dto.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/video/stream")
public class VideoStreamController {

    @Autowired
    @Qualifier("bilibiliVideoServiceImpl")
    private BilibiliVideoService bilibiliVideoService;

    /**
     * 获取视频播放信息（通过bvid）
     * @param bvid 视频BV号
     * @param cid 视频CID
     * @param qn 视频清晰度标识（可选）
     * @param fnval 视频流格式标识（可选）
     * @param fnver 视频流版本标识（可选）
     * @param fourk 是否允许4K视频（可选）
     * @return 视频播放信息
     */
    @GetMapping("/playurl")
    public ResponseResult<JSONObject> getVideoPlayUrl(
            @RequestParam String bvid,
            @RequestParam String cid,
            @RequestParam(required = false) String qn,
            @RequestParam(required = false) String fnval,
            @RequestParam(required = false) String fnver,
            @RequestParam(required = false) String fourk) {
        try {
            log.info("获取视频播放信息: bvid={}, cid={}, qn={}, fnval={}, fnver={}, fourk={}", 
                    bvid, cid, qn, fnval, fnver, fourk);
            
            JSONObject data = bilibiliVideoService.getVideoPlayInfo(bvid, cid, qn, fnval, fnver, fourk);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取视频播放信息失败: bvid={}, cid={}", bvid, cid, e);
            return ResponseResult.error("获取视频播放信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取视频播放信息（通过aid）
     * @param aid 视频AV号
     * @param cid 视频CID
     * @param qn 视频清晰度标识（可选）
     * @param fnval 视频流格式标识（可选）
     * @param fnver 视频流版本标识（可选）
     * @param fourk 是否允许4K视频（可选）
     * @return 视频播放信息
     */
    @GetMapping("/playurl/aid")
    public ResponseResult<JSONObject> getVideoPlayUrlByAid(
            @RequestParam String aid,
            @RequestParam String cid,
            @RequestParam(required = false) String qn,
            @RequestParam(required = false) String fnval,
            @RequestParam(required = false) String fnver,
            @RequestParam(required = false) String fourk) {
        try {
            log.info("获取视频播放信息: aid={}, cid={}, qn={}, fnval={}, fnver={}, fourk={}", 
                    aid, cid, qn, fnval, fnver, fourk);
            
            JSONObject data = bilibiliVideoService.getVideoPlayInfoByAid(aid, cid, qn, fnval, fnver, fourk);
            return ResponseResult.success(data);
        } catch (Exception e) {
            log.error("获取视频播放信息失败: aid={}, cid={}", aid, cid, e);
            return ResponseResult.error("获取视频播放信息失败: " + e.getMessage());
        }
    }
}