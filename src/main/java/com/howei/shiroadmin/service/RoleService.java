package com.howei.shiroadmin.service;

import com.howei.shiroadmin.model.Role;

import java.util.List;

public interface RoleService {
    List<Role> getByUserId(Integer uid);
}
