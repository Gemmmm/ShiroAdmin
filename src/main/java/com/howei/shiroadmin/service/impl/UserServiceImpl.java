package com.howei.shiroadmin.service.impl;

import com.howei.shiroadmin.dao.UserMapper;
import com.howei.shiroadmin.model.User;
import com.howei.shiroadmin.model.UserExample;
import com.howei.shiroadmin.model.UserExample.Criteria;
import com.howei.shiroadmin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper mapper;

    @Override
    public User getByUsernameAndPassword(String username, String password) {
        UserExample example = new UserExample();
        Criteria criteria = example.createCriteria();
        criteria.andUsernameEqualTo(username);
        criteria.andPasswordEqualTo(password);
        List<User> list = mapper.selectByExample(example);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public int insert(User user) {
        return mapper.insertSelective(user);
    }

    @Override
    public int delete(Integer id) {
        return mapper.deleteByPrimaryKey(id);
    }

    @Override
    public List<User> getAll() {
        return mapper.selectByExample(null);
    }

    @Override
    public User getByUsername(String username) {
        UserExample example = new UserExample();
        Criteria criteria = example.createCriteria();
        criteria.andUsernameEqualTo(username);
        System.out.println(username);
        List<User> list = mapper.selectByExample(example);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public int update(User user) {
        return mapper.updateByPrimaryKeySelective(user);
    }
}
