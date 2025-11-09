package com.tbw.cut.controller;

import com.tbw.cut.dto.BilibiliCollectionDTO;
import com.tbw.cut.dto.BilibiliPartitionDTO;
import com.tbw.cut.service.BilibiliCollectionAndPartitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bilibili")
public class BilibiliCollectionAndPartitionController {

    @Autowired
    private BilibiliCollectionAndPartitionService collectionAndPartitionService;

    /**
     * 获取用户的所有合集列表
     * @param mid 用户mid
     * @return 合集列表
     */
    @GetMapping("/collections")
    public SubmissionTaskController.Result<List<BilibiliCollectionDTO>> getUserCollections(
            @RequestParam Long mid) {
        try {
            List<BilibiliCollectionDTO> collections = collectionAndPartitionService.getUserCollections(mid);
            return SubmissionTaskController.Result.success(collections);
        } catch (Exception e) {
            log.error("获取用户合集列表失败", e);
            return SubmissionTaskController.Result.error("获取用户合集列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取B站所有视频分区
     * @return 分区列表
     */
    @GetMapping("/partitions")
    public SubmissionTaskController.Result<List<BilibiliPartitionDTO>> getAllPartitions() {
        try {
            List<BilibiliPartitionDTO> partitions = collectionAndPartitionService.getAllPartitions();
            return SubmissionTaskController.Result.success(partitions);
        } catch (Exception e) {
            log.error("获取B站分区列表失败", e);
            return SubmissionTaskController.Result.error("获取B站分区列表失败: " + e.getMessage());
        }
    }
}