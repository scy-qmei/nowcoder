package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public User getUserById(int userId) {
        return userMapper.selectById(userId);
    }
}
