package com.tbw.cut.service;

import com.tbw.cut.entity.Anchor;

public interface LiveStreamService {
    
    /**
     * Record live stream for anchor
     * @param anchor Anchor information
     * @param streamUrl Live stream URL
     * @return Recorded file path if successful, null otherwise
     */
    String recordLiveStream(Anchor anchor, String streamUrl);
    
    /**
     * Get live stream URL for anchor
     * @param anchor Anchor information
     * @return Live stream URL if successful, null otherwise
     */
    String getLiveStreamUrl(Anchor anchor);
}