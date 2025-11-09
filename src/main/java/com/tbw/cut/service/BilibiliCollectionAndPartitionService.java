package com.tbw.cut.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.dto.BilibiliCollectionDTO;
import com.tbw.cut.dto.BilibiliPartitionDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface BilibiliCollectionAndPartitionService {

    /**
     * 获取用户的所有合集列表
     * @param mid 用户mid
     * @return 合集列表
     */
    List<BilibiliCollectionDTO> getUserCollections(Long mid);


    /**
     * 获取B站所有视频分区
     * @return 分区列表
     */
    List<BilibiliPartitionDTO> getAllPartitions();
}
