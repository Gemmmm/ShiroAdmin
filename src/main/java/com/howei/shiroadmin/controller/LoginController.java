package com.howei.shiroadmin.controller;

import com.howei.shiroadmin.model.User;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ByteSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/")
public class LoginController {


    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String loginForm(Model model, HttpServletRequest request, String username, boolean rememberMe,String password, HttpSession session) {

        //对密码进行加密
        //password=new SimpleHash("md5", password, ByteSource.Util.bytes(username.toLowerCase() + "shiro"),2).toHex();

        UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(username, password,rememberMe);
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(usernamePasswordToken);
            User user = (User) subject.getPrincipal();
            session.setAttribute("user", user);
            model.addAttribute("user", user);
            return "redirect:index";
        } catch (Exception e) {
            //登录失败从request中获取shiro处理的异常信息 shiroLoginFailure:就是shiro异常类的全类名
            String exception = (String) request.getAttribute("shiroLoginFailure");
            model.addAttribute("msg", e.getMessage());
            return "login";
        }

    }

    @GetMapping("/index")
    public String index(HttpSession session, Model model) {
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        if (user == null) {
            return "login";
        } else {
            model.addAttribute("user",user);
            return "index";
        }
    }

    @GetMapping("/logout")
    public String logout(Model model) {
        Subject subject = SecurityUtils.getSubject();
        subject.logout();
        model.addAttribute("msg", "安全退出");
        return "login";
    }



}
