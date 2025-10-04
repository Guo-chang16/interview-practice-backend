package com.guochang.interviewpracticebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guochang.interviewpracticebackend.mapper.UserMapper;
import com.guochang.interviewpracticebackend.model.entity.User;
import com.guochang.interviewpracticebackend.service.UserService;
import org.springframework.stereotype.Service;

/**
* @author 31179
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-10-04 11:19:57
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

}




