package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author mqx
 * @date 2020/3/25 11:15
 */
@Service
public class UserServiceImpl implements UserService {
    // 引入mapper
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {
        // select * from user_info where login_name=? and passwd=?
        // 密码是加密的，先获取用户输入的密码，然后对其进行加密。
        String passwd = userInfo.getPasswd();
        String newPwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        // 创建查询对象
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("login_name",userInfo.getLoginName());
        // 密码要传递加密之后的
        userInfoQueryWrapper.eq("passwd",newPwd);
        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        if (info!=null){
            // 直接返回用户对象
            return info;
        }
        // 返回空
        return null;
    }
}
