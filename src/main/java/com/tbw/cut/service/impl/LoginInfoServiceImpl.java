package com.tbw.cut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tbw.cut.entity.LoginInfo;
import com.tbw.cut.mapper.LoginInfoMapper;
import com.tbw.cut.service.LoginInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class LoginInfoServiceImpl implements LoginInfoService {
    
    @Autowired
    private LoginInfoMapper loginInfoMapper;
    
    @Override
    public LoginInfo saveOrUpdate(LoginInfo loginInfo) {
        // 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        if (loginInfo.getCreateTime() == null) {
            loginInfo.setCreateTime(now);
        }
        loginInfo.setUpdateTime(now);
        
        // 检查是否已存在该用户的登录信息
        LoginInfo existingInfo = loginInfoMapper.selectByUserId(loginInfo.getUserId());
        if (existingInfo != null) {
            // 更新现有记录
            loginInfo.setId(existingInfo.getId());
            loginInfoMapper.updateById(loginInfo);
            log.info("更新用户 {} 的登录信息", loginInfo.getUserId());
            return loginInfoMapper.selectById(loginInfo.getId());
        } else {
            // 新增记录
            loginInfoMapper.insert(loginInfo);
            log.info("新增用户 {} 的登录信息", loginInfo.getUserId());
            return loginInfoMapper.selectById(loginInfo.getId());
        }
    }
    
    @Override
    public LoginInfo getByUserId(Long userId) {
        return loginInfoMapper.selectByUserId(userId);
    }
    
    @Override
    public boolean existsByUserId(Long userId) {
        return loginInfoMapper.existsByUserId(userId);
    }
    
    @Override
    public void deleteByUserId(Long userId) {
        LoginInfo loginInfo = loginInfoMapper.selectByUserId(userId);
        if (loginInfo != null) {
            loginInfoMapper.deleteById(loginInfo.getId());
            log.info("删除用户 {} 的登录信息", userId);
        }
    }
}