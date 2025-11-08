package com.tbw.cut.config;

import com.tbw.cut.service.AnchorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
public class ScheduledConfig {
    
    @Autowired
    private AnchorService anchorService;
    
    /**
     * 每5分钟检查一次主播直播状态
     */
//    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void checkAnchorLiveStatus() {
        try {
            log.info("开始检查主播直播状态");
            anchorService.checkLiveStatus();
            log.info("主播直播状态检查完成");
        } catch (Exception e) {
            log.error("检查主播直播状态时发生异常", e);
        }
    }
}