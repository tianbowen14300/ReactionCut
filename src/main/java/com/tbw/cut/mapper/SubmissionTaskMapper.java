package com.tbw.cut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tbw.cut.entity.SubmissionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SubmissionTaskMapper extends BaseMapper<SubmissionTask> {
    
    @Select("SELECT * FROM submission_task ORDER BY created_at DESC")
    List<SubmissionTask> findAllOrderByCreatedAtDesc();
    
    @Select("SELECT * FROM submission_task WHERE status = #{status} ORDER BY created_at DESC")
    List<SubmissionTask> findByStatusOrderByCreatedAtDesc(@Param("status") SubmissionTask.TaskStatus status);
    
    @Select("SELECT COUNT(*) FROM submission_task WHERE status = #{status}")
    int countByStatus(@Param("status") SubmissionTask.TaskStatus status);
}