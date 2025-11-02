package com.tbw.cut.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tbw.cut.entity.LoginInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface LoginInfoMapper extends BaseMapper<LoginInfo> {
    /**
     * 根据用户ID查询登录信息
     * @param userId 用户ID
     * @return 登录信息
     */
    @Select("SELECT * FROM login_info WHERE user_id = #{userId}")
    LoginInfo selectByUserId(@Param("userId") Long userId);
    
    /**
     * 检查用户是否存在登录信息
     * @param userId 用户ID
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM login_info WHERE user_id = #{userId}")
    boolean existsByUserId(@Param("userId") Long userId);
}