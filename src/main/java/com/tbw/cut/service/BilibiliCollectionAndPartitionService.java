package com.tbw.cut.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tbw.cut.bilibili.BilibiliApiClient;
import com.tbw.cut.dto.BilibiliCollectionDTO;
import com.tbw.cut.dto.BilibiliPartitionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BilibiliCollectionAndPartitionService {

    @Autowired
    private BilibiliApiClient bilibiliApiClient;

    /**
     * 获取用户的所有合集列表
     * @param mid 用户mid
     * @return 合集列表
     */
    public List<BilibiliCollectionDTO> getUserCollections(Long mid) {
        List<BilibiliCollectionDTO> collections = new ArrayList<>();
        
        try {
            // 使用正确的B站API获取合集列表
            Map<String, String> params = new HashMap<>();
            params.put("pn", "1");
            params.put("ps", "100"); // 获取前100个合集
            params.put("order", "desc");
            params.put("sort", "mtime");
            params.put("filter", "1");
            
            String response = bilibiliApiClient.get(
                "https://member.bilibili.com/x2/creative/web/seasons", params);
            
            JSONObject jsonObject = JSON.parseObject(response);
            if (jsonObject.getIntValue("code") == 0) {
                JSONObject data = jsonObject.getJSONObject("data");
                if (data != null) {
                    JSONArray seasons = data.getJSONArray("seasons");
                    if (seasons != null && !seasons.isEmpty()) {
                        for (int i = 0; i < seasons.size(); i++) {
                            JSONObject item = seasons.getJSONObject(i);
                            BilibiliCollectionDTO collection = new BilibiliCollectionDTO();
                            collection.setSeasonId(item.getJSONObject("season").getLong("id"));
                            collection.setName(item.getJSONObject("season").getString("title"));
                            collection.setCover(item.getJSONObject("season").getString("cover"));
                            collection.setDescription(item.getJSONObject("season").getString("desc"));
                            collections.add(collection);
                        }
                    }
                }
            } else {
                log.error("获取用户合集列表失败，错误码: {}, 错误信息: {}", 
                    jsonObject.getIntValue("code"), jsonObject.getString("message"));
            }
        } catch (Exception e) {
            log.error("获取用户合集列表异常", e);
        }
        
        return collections;
    }

    /**
     * 获取B站所有视频分区
     * @return 分区列表
     */
    public List<BilibiliPartitionDTO> getAllPartitions() {
        List<BilibiliPartitionDTO> partitions = new ArrayList<>();
        
        try {
            // 使用正确的B站API获取分区数据
            Map<String, String> params = new HashMap<>();
            params.put("t", String.valueOf(System.currentTimeMillis()));
            
            String response = bilibiliApiClient.get(
                "https://member.bilibili.com/x/vupre/web/archive/human/type2/list", params);
            
            JSONObject jsonObject = JSON.parseObject(response);
            if (jsonObject.getIntValue("code") == 0) {
                JSONObject data = jsonObject.getJSONObject("data");
                if (data != null) {
                    JSONArray types = data.getJSONArray("type_list");
                    if (types != null && !types.isEmpty()) {
                        // 解析分区数据
                        for (int i = 0; i < types.size(); i++) {
                            JSONObject type = types.getJSONObject(i);
                            int tid = type.getIntValue("id");
                            String name = type.getString("name");
                            
                            // 只添加主分区和子分区，不区分层级关系
                            BilibiliPartitionDTO partition = new BilibiliPartitionDTO();
                            partition.setTid(tid);
                            partition.setName(name);
                            partitions.add(partition);
                        }
                    }
                }
            } else {
                log.error("获取B站分区列表失败，错误码: {}, 错误信息: {}", 
                    jsonObject.getIntValue("code"), jsonObject.getString("message"));
                // 如果API调用失败，使用默认的分区数据
                partitions = getDefaultPartitions();
            }
        } catch (Exception e) {
            log.error("获取B站分区列表异常", e);
            // 如果出现异常，使用默认的分区数据
            partitions = getDefaultPartitions();
        }
        
        return partitions;
    }
    
    /**
     * 获取默认的分区数据（当API调用失败时使用）
     * @return 默认分区列表
     */
    private List<BilibiliPartitionDTO> getDefaultPartitions() {
        List<BilibiliPartitionDTO> partitions = new ArrayList<>();
        
        // 创建一些默认分区
        BilibiliPartitionDTO animation = new BilibiliPartitionDTO();
        animation.setTid(1);
        animation.setName("动画");
        partitions.add(animation);
        
        BilibiliPartitionDTO game = new BilibiliPartitionDTO();
        game.setTid(4);
        game.setName("游戏");
        partitions.add(game);
        
        BilibiliPartitionDTO knowledge = new BilibiliPartitionDTO();
        knowledge.setTid(36);
        knowledge.setName("知识");
        partitions.add(knowledge);
        
        BilibiliPartitionDTO tech = new BilibiliPartitionDTO();
        tech.setTid(188);
        tech.setName("科技");
        partitions.add(tech);
        
        return partitions;
    }
}