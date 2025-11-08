package com.tbw.cut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tbw.cut.entity.VideoClip;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface VideoClipMapper extends BaseMapper<VideoClip> {
    
    @Select("SELECT * FROM video_clip WHERE task_id = #{taskId} ORDER BY sequence ASC")
    List<VideoClip> findByTaskIdOrderBySequence(@Param("taskId") String taskId);
}