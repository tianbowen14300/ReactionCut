package com.tbw.cut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tbw.cut.entity.TaskSourceVideo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.UUID;

@Mapper
public interface TaskSourceVideoMapper extends BaseMapper<TaskSourceVideo> {
    
    @Select("SELECT * FROM task_source_video WHERE task_id = #{taskId} ORDER BY sort_order ASC")
    List<TaskSourceVideo> findByTaskIdOrderBySortOrder(@Param("taskId") String taskId);
}