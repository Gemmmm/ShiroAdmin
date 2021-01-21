package com.howei.shiroadmin.service.impl;

import com.howei.shiroadmin.dao.RoleMapper;
import com.howei.shiroadmin.model.Role;
import com.howei.shiroadmin.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {


    @Autowired(required = false)
    private RoleMapper mapper;

    @Override
    public List<Role> getByUserId(Integer uid) {

        return mapper.selectByUserId(uid);
    }
}
