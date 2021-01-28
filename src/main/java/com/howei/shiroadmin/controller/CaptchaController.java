package com.howei.shiroadmin.controller;

import com.howei.shiroadmin.util.CaptchaUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;

@Controller
public class CaptchaController {

    public static final String KEY_CAPTCHA = "KEY_CAPTCHA";

    @GetMapping("/Captcha.jpg")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) {

        response.setContentType("image/jpeg");
        //不缓存此内容
        response.setHeader("Pragma","No-cache");

        response.setDateHeader("Expire",0);
        try {
            HttpSession session=request.getSession();
            CaptchaUtil tool = new CaptchaUtil();
            StringBuffer code=new StringBuffer();
            BufferedImage image = tool.genRandomCodeImage(code);
            session.removeAttribute(KEY_CAPTCHA);
            session.setAttribute(KEY_CAPTCHA,code.toString());
            ImageIO.write(image,"JPEG",response.getOutputStream());
        }catch (Exception e){

        }
    }
}
