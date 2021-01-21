package com.howei.shiroadmin.service;

import com.howei.shiroadmin.model.Permission;
import com.howei.shiroadmin.model.Role;

import java.util.List;

public interface PermissionService {

    List<Permission> getByRoleId(Integer roleId);
}
