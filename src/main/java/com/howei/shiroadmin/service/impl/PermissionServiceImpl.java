package com.howei.shiroadmin.service.impl;

import com.howei.shiroadmin.dao.PermissionMapper;
import com.howei.shiroadmin.model.Permission;
import com.howei.shiroadmin.model.Role;
import com.howei.shiroadmin.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionServiceImpl implements PermissionService {


    @Autowired
    private PermissionMapper mapper;

    @Override
    public List<Permission> getByRoleId(Integer roleId) {


        return mapper.selectByRoleId(roleId);
    }
}
