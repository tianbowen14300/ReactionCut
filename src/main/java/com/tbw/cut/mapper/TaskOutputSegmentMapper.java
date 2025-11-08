package com.tbw.cut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tbw.cut.entity.TaskOutputSegment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;
import java.util.UUID;

@Mapper
public interface TaskOutputSegmentMapper extends BaseMapper<TaskOutputSegment> {
    
    @Select("SELECT * FROM task_output_segment WHERE task_id = #{taskId} ORDER BY part_order ASC")
    List<TaskOutputSegment> findByTaskIdOrderByPartOrder(@Param("taskId") String taskId);
    
    @Select("SELECT * FROM task_output_segment WHERE task_id = #{taskId} AND upload_status = #{status}")
    List<TaskOutputSegment> findByTaskIdAndUploadStatus(@Param("taskId") String taskId, @Param("status") TaskOutputSegment.UploadStatus status);
    
    @Update("UPDATE task_output_segment SET upload_status = #{status}, cid = #{cid} WHERE segment_id = #{segmentId}")
    void updateUploadStatusAndCid(@Param("segmentId") UUID segmentId, @Param("status") TaskOutputSegment.UploadStatus status, @Param("cid") Long cid);
}