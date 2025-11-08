package com.tbw.cut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tbw.cut.entity.MergedVideo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MergedVideoMapper extends BaseMapper<MergedVideo> {
    
    @Select("SELECT * FROM merged_video WHERE task_id = #{taskId} ORDER BY create_time DESC")
    List<MergedVideo> findByTaskIdOrderByCreateTime(@Param("taskId") String taskId);
}