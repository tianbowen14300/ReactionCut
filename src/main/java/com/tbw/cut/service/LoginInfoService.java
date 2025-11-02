package com.tbw.cut.service;

import com.tbw.cut.entity.LoginInfo;

public interface LoginInfoService {
    /**
     * 保存或更新登录信息
     * @param loginInfo 登录信息
     * @return 保存后的登录信息
     */
    LoginInfo saveOrUpdate(LoginInfo loginInfo);
    
    /**
     * 根据用户ID获取登录信息
     * @param userId 用户ID
     * @return 登录信息
     */
    LoginInfo getByUserId(Long userId);
    
    /**
     * 检查用户是否存在登录信息
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByUserId(Long userId);
    
    /**
     * 删除登录信息
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}