package com.howei.shiroadmin.controller;

import com.howei.shiroadmin.model.User;
import com.howei.shiroadmin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
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
        log.info("开始查询");
        List<User> users = userService.getAll();
        String result = "";
        for (User user : users) {
            result += user.toString();
        }
        return result;
    }

}
