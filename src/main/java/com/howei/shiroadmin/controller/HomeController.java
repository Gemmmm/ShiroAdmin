package com.howei.shiroadmin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("error404")
    public String error404(){
        return "error404";
    }
    @GetMapping("error500")
    public String error500(){
        return "error500";
    }
    @GetMapping("/unauthorized")
    public String unauthorized() {
        return "unauthorized";
    }


}
