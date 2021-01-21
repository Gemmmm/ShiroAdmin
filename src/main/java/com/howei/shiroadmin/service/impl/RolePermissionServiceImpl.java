package com.howei.shiroadmin.service.impl;

import com.howei.shiroadmin.dao.RolePermissionMapper;
import com.howei.shiroadmin.service.RolePermissionService;
import com.howei.shiroadmin.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionServiceImpl implements RolePermissionService {


    @Autowired(required = false)
    private RolePermissionMapper mapper;
}
