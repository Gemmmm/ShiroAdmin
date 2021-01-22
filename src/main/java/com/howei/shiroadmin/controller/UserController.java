package com.howei.shiroadmin.controller;

import com.howei.shiroadmin.config.shiro.ShiroRealm;
import com.howei.shiroadmin.model.RolePermission;
import com.howei.shiroadmin.model.User;
import com.howei.shiroadmin.service.RolePermissionService;
import com.howei.shiroadmin.service.RoleService;
import com.howei.shiroadmin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private RolePermissionService rpService;

    @GetMapping("/add")
    public String login(String username, String password) {
        User user = new User();

        user.setUsername(username);
        user.setPassword(password);
        user.setName(username);
        int count = userService.insert(user);
        return "添加用户成功";
    }

    @GetMapping("/del/{id}")
    public String del(@PathVariable Integer id) {
        int count = userService.delete(id);
        return "删除用户成功";
    }

    @RequiresPermissions("user:view")
    @GetMapping("view")
    public String view() {
        String result = "";
        log.info("user:view开始查询");
        List<User> users = userService.getAll();
        for (User user : users) {
            result += user.toString();
        }
        return result;
    }

    @RequiresPermissions(value = "user:view1", logical = Logical.AND)
    @GetMapping("view1")
    public String view1() {
        String result = "";
        log.info("user:view1开始查询");
        List<User> users = userService.getAll();
        for (User user : users) {
            result += user.toString();
        }
        return result;
    }

    /**
     *
     */
    @GetMapping("/addPermission")
    public String addPermission() {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(1);
        rolePermission.setPermissionId(3);
        int count = rpService.insert(rolePermission);
        DefaultWebSecurityManager securityManager = (DefaultWebSecurityManager) SecurityUtils.getSecurityManager();
        ShiroRealm shiroRealm = (ShiroRealm) securityManager.getRealms().iterator().next();
        //清楚所有登录者会的缓存权限
        shiroRealm.clearAllCache();
        //清除当前登录者的缓存权限
        //shiroRealm.getAuthorizationCache().remove(SecurityUtils.getSubject().getPrincipals());
        return "给admin用户添加 userInfo:del 权限成功";
    }

}
