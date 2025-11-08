package com.tbw.cut.service;

import com.tbw.cut.entity.SubmissionTask;
import com.tbw.cut.entity.TaskOutputSegment;
import java.util.List;
import java.util.UUID;

public interface BilibiliSubmissionService {
    
    /**
     * 上传分段文件到B站
     * @param taskId 任务ID
     * @param segments 分段列表
     * @return 是否上传成功
     */
    boolean uploadSegments(String taskId, List<TaskOutputSegment> segments);
    
    /**
     * 提交视频到B站
     * @param task 投稿任务
     * @param segments 分段列表
     * @return B站视频ID(BVID)
     */
    String submitVideo(SubmissionTask task, List<TaskOutputSegment> segments);
}