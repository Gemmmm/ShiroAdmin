package com.howei.shiroadmin.controller;

import com.howei.shiroadmin.config.shiro.RetryLimitHashedCredentialsMatcher;
import com.howei.shiroadmin.model.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/")
@Slf4j
public class LoginController {
//
//    @Autowired
//    private RetryLimitHashedCredentialsMatcher retryLimitHashedCredentialsMatcher;





    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String loginForm(Model model, HttpServletRequest request, String username, boolean rememberMe, String password,String captcha,  HttpSession session) {

        //对密码进行加密
        //password=new SimpleHash("md5", password, ByteSource.Util.bytes(username.toLowerCase() + "shiro"),2).toHex();

        Subject subject = SecurityUtils.getSubject();
        String sessionCaptcha= (String) subject.getSession().getAttribute(CaptchaController.KEY_CAPTCHA);

//        if(captcha==null||"".equals(captcha)||!captcha.equalsIgnoreCase(sessionCaptcha)){
//            model.addAttribute("msg","验证码错误！");
//            return "login";
//        }
        UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(username, password, rememberMe);

        try {
            subject.login(usernamePasswordToken);
            User user = (User) subject.getPrincipal();
            session.setAttribute("user", user);
            model.addAttribute("user", user);
            return "redirect:index";
        } catch (Exception e) {
            //登录失败从request中获取shiro处理的异常信息 shiroLoginFailure:就是shiro异常类的全类名
            String exception = (String) request.getAttribute("shiroLoginFailure");
            if (e instanceof UnknownAccountException) {
                model.addAttribute("msg", "没有用户");
            }
            if (e instanceof IncorrectCredentialsException) {
                model.addAttribute("msg", "用户名或者密码错误");
            }
            if (e instanceof LockedAccountException) {
                model.addAttribute("msg", "账户被锁");
            }
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
            model.addAttribute("user", user);
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


    @GetMapping("/unlockAddount")
    public String unlockAccount(Model model,String username) {
        log.info("开始解锁");
        if(username==null||"".equals(username)){
            username="admin";
        }
        //retryLimitHashedCredentialsMatcher.unlockAccount(username);
        model.addAttribute("msg", username+"用户解锁成功");
        log.info("解锁成功");
        return "login";
    }



}
