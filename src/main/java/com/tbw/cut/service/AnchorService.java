package com.tbw.cut.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tbw.cut.entity.Anchor;
import com.tbw.cut.dto.AnchorSubscribeDTO;
import java.util.List;

public interface AnchorService extends IService<Anchor> {
    
    /**
     * 批量订阅主播
     * @param dto 订阅信息
     * @return 是否成功
     */
    boolean subscribeAnchors(AnchorSubscribeDTO dto);
    
    /**
     * 检查主播直播状态
     */
    void checkLiveStatus();
    
    /**
     * 根据UID获取主播信息
     * @param uid 主播UID
     * @return 主播信息
     */
    Anchor getAnchorByUid(String uid);
    
    /**
     * 获取所有订阅的主播
     * @return 主播列表
     */
    List<Anchor> getAllSubscribedAnchors();
}