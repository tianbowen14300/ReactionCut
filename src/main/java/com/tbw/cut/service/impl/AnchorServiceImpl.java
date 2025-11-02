package com.tbw.cut.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tbw.cut.bilibili.BilibiliService;
import com.tbw.cut.entity.Anchor;
import com.tbw.cut.mapper.AnchorMapper;
import com.tbw.cut.service.AnchorService;
import com.tbw.cut.service.LiveStreamService;
import com.tbw.cut.dto.AnchorSubscribeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AnchorServiceImpl extends ServiceImpl<AnchorMapper, Anchor> implements AnchorService {
    
    @Autowired
    private LiveStreamService liveStreamService;
    
    @Autowired
    private BilibiliService bilibiliService;
    
    @Override
    public boolean subscribeAnchors(AnchorSubscribeDTO dto) {
        try {
            List<String> uids = dto.getUids();
            for (String uid : uids) {
                // Check if already subscribed
                Anchor existingAnchor = this.lambdaQuery().eq(Anchor::getUid, uid).one();
                if (existingAnchor == null) {
                    // Add new subscription
                    Anchor anchor = new Anchor();
                    anchor.setUid(uid);
                    anchor.setLiveStatus(0); // Not live by default
                    anchor.setCreateTime(LocalDateTime.now());
                    anchor.setUpdateTime(LocalDateTime.now());
                    this.save(anchor);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to subscribe anchors", e);
            return false;
        }
    }
    
    @Override
    public void checkLiveStatus() {
        // Get all subscribed anchors
        List<Anchor> anchors = this.list();
        for (Anchor anchor : anchors) {
            // Use Bilibili API to check live status
            log.info("Checking live status for anchor: {}", anchor.getUid());
            
            try {
                // Get live status from Bilibili API using multiple methods
                JSONObject liveInfo = null;
                
                // Method 1: Try to get room info using user ID (getRoomInfoOld)
                try {
                    liveInfo = bilibiliService.getRoomInfoOld(anchor.getUid());
                } catch (Exception e1) {
                    log.warn("Failed to get room info old for anchor: {}", anchor.getUid());
                    
                    // Method 2: Try to get room init info using user ID
                    try {
                        liveInfo = bilibiliService.getRoomInitInfo(anchor.getUid());
                    } catch (Exception e2) {
                        log.warn("Failed to get room init info for anchor: {}", anchor.getUid());
                        
                        // Method 3: Try batch status check
                        try {
                            JSONObject batchResult = bilibiliService.getLiveStatusBatch(anchor.getUid());
                            if (batchResult != null && batchResult.getJSONObject("data") != null) {
                                // Extract the specific user's info from batch result
                                JSONObject userData = batchResult.getJSONObject("data").getJSONObject(anchor.getUid());
                                if (userData != null) {
                                    liveInfo = new JSONObject();
                                    liveInfo.put("data", userData);
                                }
                            }
                        } catch (Exception e3) {
                            log.warn("Failed to get batch live status for anchor: {}", anchor.getUid());
                        }
                    }
                }
                
                if (liveInfo != null) {
                    // Extract live status from response
                    boolean isLive = extractLiveStatusFromResponse(liveInfo);
                    
                    if (isLive && anchor.getLiveStatus() != 1) {
                        // Anchor just went live, start recording
                        startRecording(anchor);
                    }
                    
                    anchor.setLiveStatus(isLive ? 1 : 0);
                    
                    // Update additional information from response
                    updateAnchorInfoFromResponse(anchor, liveInfo);
                }
            } catch (Exception e) {
                log.error("Failed to check live status for anchor: {}", anchor.getUid(), e);
            }
            
            anchor.setLastCheckTime(LocalDateTime.now());
            anchor.setUpdateTime(LocalDateTime.now());
            this.updateById(anchor);
        }
    }
    
    @Override
    public Anchor getAnchorByUid(String uid) {
        return this.lambdaQuery().eq(Anchor::getUid, uid).one();
    }
    
    @Override
    public List<Anchor> getAllSubscribedAnchors() {
        return this.list();
    }
    
    /**
     * Extract live status from Bilibili API response
     * @param liveInfo Live info response
     * @return True if live, false otherwise
     */
    private boolean extractLiveStatusFromResponse(JSONObject liveInfo) {
        try {
            // Try different response structures
            // Method 1: Check data object
            JSONObject data = liveInfo.getJSONObject("data");
            if (data != null) {
                // Check live_status field
                Integer liveStatus = data.getInteger("live_status");
                if (liveStatus != null) {
                    return liveStatus == 1;
                }
                
                // Check roomStatus field
                Integer roomStatus = data.getInteger("roomStatus");
                if (roomStatus != null) {
                    return roomStatus == 1;
                }
            }
            
            // Method 2: Check root level live_status
            Integer liveStatus = liveInfo.getInteger("live_status");
            if (liveStatus != null) {
                return liveStatus == 1;
            }
            
            // Method 3: Check root level roomStatus
            Integer roomStatus = liveInfo.getInteger("roomStatus");
            if (roomStatus != null) {
                return roomStatus == 1;
            }
        } catch (Exception e) {
            log.error("Failed to extract live status from response", e);
        }
        return false;
    }
    
    /**
     * Update anchor information from Bilibili API response
     * @param anchor Anchor entity
     * @param liveInfo Live info response
     */
    private void updateAnchorInfoFromResponse(Anchor anchor, JSONObject liveInfo) {
        try {
            // Try to extract anchor information from different response structures
            JSONObject data = liveInfo.getJSONObject("data");
            if (data != null) {
                // Extract title
                String title = data.getString("title");
                if (title != null) {
                    // In a real implementation, you might want to store this in the Anchor entity
                    log.info("Anchor {} title: {}", anchor.getUid(), title);
                }
                
                // Extract online count
                Integer online = data.getInteger("online");
                if (online != null) {
                    log.info("Anchor {} online: {}", anchor.getUid(), online);
                }
                
                // Extract room id
                Long roomId = data.getLong("room_id");
                if (roomId != null) {
                    log.info("Anchor {} room id: {}", anchor.getUid(), roomId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to update anchor info from response", e);
        }
    }
    
    /**
     * Start recording live stream
     * @param anchor Anchor information
     */
    private void startRecording(Anchor anchor) {
        try {
            // Get live stream URL
            String streamUrl = liveStreamService.getLiveStreamUrl(anchor);
            
            if (streamUrl != null && !streamUrl.isEmpty()) {
                log.info("Starting to record live stream for anchor: {}", anchor.getUid());
                
                // Record live stream asynchronously
                new Thread(() -> {
                    String recordedFilePath = liveStreamService.recordLiveStream(anchor, streamUrl);
                    if (recordedFilePath != null) {
                        log.info("Successfully recorded live stream for anchor: {}, file: {}", 
                                anchor.getUid(), recordedFilePath);
                    } else {
                        log.error("Failed to record live stream for anchor: {}", anchor.getUid());
                    }
                }).start();
            } else {
                log.warn("Failed to get live stream URL for anchor: {}", anchor.getUid());
            }
        } catch (Exception e) {
            log.error("Failed to start recording for anchor: {}", anchor.getUid(), e);
        }
    }
}