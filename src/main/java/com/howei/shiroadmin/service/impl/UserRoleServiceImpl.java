package com.howei.shiroadmin.service.impl;

import com.howei.shiroadmin.dao.UserRoleMapper;
import com.howei.shiroadmin.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    @Autowired
    private UserRoleMapper mapper;
}
