package com.howei.shiroadmin.config;

import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import java.util.LinkedHashMap;
import java.util.Properties;

@Configuration
public class ShiroConfig {


    /**
     * ShiroFilterFactoryBean 处理拦截资源文件问题
     * 注意 初始化ShiroFilterFactoryBean的时候需要注入SecurityManager
     * Web应用中Shiro克控制的Web请求必须经过Shiro主过滤器的拦截
     *
     * @param securityManager
     * @return
     */
    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shiroFilterFactoryBean(@Qualifier("securityManager") SecurityManager securityManager) {

        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        //必须设置SecurityManager，Shiro的核心安全接口
        shiroFilterFactoryBean.setSecurityManager(securityManager);

        //这里的/login是后台的接口，不是页面，如果不设置 会自动寻找Web工程目录下的 “/login”页面
        shiroFilterFactoryBean.setLoginUrl("/login");
        //登录成功之后的跳转连接
        shiroFilterFactoryBean.setSuccessUrl("/index");

        //未授权页面
        shiroFilterFactoryBean.setUnauthorizedUrl("/unauthorized");

        LinkedHashMap<String, String> filterLinkedHashMap = new LinkedHashMap<>();
        //配置不登陆就可以访问的资源， anon表示资源都可以匿名访问
        filterLinkedHashMap.put("/login", "anon");
        filterLinkedHashMap.put("/", "anon");
        filterLinkedHashMap.put("/css/**", "anon");
        filterLinkedHashMap.put("/js/**", "anon");
        filterLinkedHashMap.put("/img/**", "anon");
        filterLinkedHashMap.put("/druid/**", "anon");
        filterLinkedHashMap.put("/logout", "logout");
        //此时访问/userInfo/del需要del权限,在自定义Realm中为用户授权。
        //filterLinkedHashMap.put("/user/del", "\"perms[\"userInfo:del\"]")

        //其他资源需要认证， auth 表示需要认证才能访问
        filterLinkedHashMap.put("/**", "authc");

        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterLinkedHashMap);
        return shiroFilterFactoryBean;
    }

    /**
     * 配置核心安全事务管理其
     *
     * @param shiroReam
     * @return
     */
    @Bean("securityManager")
    public SecurityManager securityManager(@Qualifier("shiroRealm") ShiroReam shiroReam) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        //配置自定义Realm
        securityManager.setRealm(shiroReam);
        return securityManager;
    }

    /**
     * shrio生命周期处理器
     *
     * @return
     */
    @Bean("lifecycleBeanPostProcessor")
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    /**
     * 身份认证realm ，自定义
     *
     * @return
     */
    @Bean
    public ShiroReam shiroRealm() {
        ShiroReam shiroRealm = new ShiroReam();
        return shiroRealm;
    }


    /**
     * 必须（thmeleaf页面使用shiro标签控制按钮是否显示）
     * 未引入thymeleaf包，Caused by: java.lang.ClassNotFoundException: org.thymeleaf.dialect.AbstractProcessorDialect
     *
     * @return
     */
    @Bean
    public ShiroDialect shiroDialect() {
        return new ShiroDialect();
    }


    /**
     * 开启shiro注解模式，可以在controller方法前加注解@RequestPermissions("user:add")
     *
     * @param securityManager
     * @return
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(@Qualifier("securityManager") SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
        return authorizationAttributeSourceAdvisor;
    }


    /**
     * 五权限页面不跳转，shiroFilterFactoryBean.setUnauthorizedUrl
     *
     * @return
     */
    @Bean
    public SimpleMappingExceptionResolver simpleMappingExceptionResolver() {
        SimpleMappingExceptionResolver simpleMappingExceptionResolver = new SimpleMappingExceptionResolver();
        Properties properties = new Properties();
        //这里的/unauthorized是页面，不是路径
        properties.setProperty("org.apache.shiro.authz.UnauthorizedException", "/unauthorized");
        properties.setProperty("org.apache.shiro.authz.UnauthenticatedException", "/unauthorized");
        simpleMappingExceptionResolver.setExceptionMappings(properties);
        return simpleMappingExceptionResolver;
    }


    /**
     * springboot whitelabel error page
     *
     * @return
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryWebServerFactoryCustomizer() {
        return factory -> {
            ErrorPage errorPage401 = new ErrorPage(HttpStatus.UNAUTHORIZED, "/unauthorized.html");
            ErrorPage errorPage404 = new ErrorPage(HttpStatus.NOT_FOUND, "/404.html");
            ErrorPage errorPage500 = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500.html");

            factory.addErrorPages(errorPage401, errorPage404, errorPage500);
        };
    }

    /**
     * cookie对象，会话cookie模板，设置为jessionid问题，与servlet容器重新定义为sid或者rememberMe
     * @return
     */

    @Bean
    public SimpleCookie rememberMeCookie() {
        //这个参数是cookie的名称，对应前端的checkboe 的name =rememberMe
        SimpleCookie simpleCookie = new SimpleCookie("rememberMe");
        //setcookie的httponly属性如果设为true的话，会增加xss防护的安全系数，他有以下特点
        //setcookie()的第七个参数
        //设为true后，只能通过http访问，javascript无法访问
        //防止xss读取cookie
        simpleCookie.setHttpOnly(true);
        simpleCookie.setPath("/");
        //记住我bookie设置30天
        simpleCookie.setMaxAge(2592000);
        return simpleCookie;

    }

    /**
     *  cookie 管理对象, 记住我功能
     * @return
     */
    @Bean
    public CookieRememberMeManager cookieRememberMeManager(){
        CookieRememberMeManager cookieRememberMeManager=new CookieRememberMeManager();
        cookieRememberMeManager.setCookie(rememberMeCookie());
        cookieRememberMeManager.setCipherKey(Base64.decode("4AvVhmFLUs0KTA3Kprsdag=="));
        return cookieRememberMeManager;
    }


    public FormAuthenticationFilter formAuthenticationFilter(){
    }



}
