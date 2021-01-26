package com.howei.shiroadmin.config.shiro;

import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.session.SessionListener;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.session.mgt.eis.SessionIdGenerator;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import javax.servlet.Filter;
import java.util.*;

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

        LinkedHashMap<String, Filter> filtersMap=new LinkedHashMap<>();
        filtersMap.put("kickout",kickoutSessionControlFilter());
        shiroFilterFactoryBean.setFilters(filtersMap);


        LinkedHashMap<String, String> filterLinkedHashMap = new LinkedHashMap<>();
        //配置不登陆就可以访问的资源， anon表示资源都可以匿名访问
        filterLinkedHashMap.put("/login", "kickout,anon");
        filterLinkedHashMap.put("/", "anon");
        filterLinkedHashMap.put("/css/**", "anon");
        filterLinkedHashMap.put("/js/**", "anon");
        filterLinkedHashMap.put("/img/**", "anon");
        filterLinkedHashMap.put("/druid/**", "anon");
        filterLinkedHashMap.put("/favicon.ico", "anon");

        filterLinkedHashMap.put("/logout", "logout");
        //此时访问/userInfo/del需要del权限,在自定义Realm中为用户授权。
        //filterLinkedHashMap.put("/user/del", "\"perms[\"userInfo:del\"]")

        //其他资源需要认证， auth 表示需要认证才能访问
        filterLinkedHashMap.put("/**", "kickout,user");

        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterLinkedHashMap);
        return shiroFilterFactoryBean;
    }

    /**
     * 配置核心安全事务管理其
     *
     * @param shiroRealm
     * @return
     */
    @Bean("securityManager")
    public SecurityManager securityManager(@Qualifier("shiroRealm") ShiroRealm shiroRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        //配置自定义Realm
        securityManager.setRealm(shiroRealm);
        //配置记住我
        securityManager.setRememberMeManager(rememberMeManager());
        //配置ehchache缓存管理
        securityManager.setCacheManager(ehCacheManager());
        //配置自定义session管理,使用ehcache或者redis
        securityManager.setSessionManager(sessionManager());

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
    public ShiroRealm shiroRealm() {
        ShiroRealm shiroRealm = new ShiroRealm();
        shiroRealm.setCachingEnabled(true);
        //启动身份验证缓存,  即缓存authenticationInfo信息,默认为False
        shiroRealm.setAuthenticationCachingEnabled(true);
        //缓存authenticationInfo信息的名称 ,在ehcache-shiro.xml中有对应的缓存的配置
        shiroRealm.setAuthenticationCacheName("authenticationCache");

        shiroRealm.setAuthorizationCachingEnabled(true);
        //authorizationInfo ,在ehcache-shiro.xml中有对应的缓存的配置
        shiroRealm.setAuthorizationCacheName("authorizationCache");
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
            ErrorPage errorPage401 = new ErrorPage(HttpStatus.UNAUTHORIZED, "/unauthorized");
            ErrorPage errorPage404 = new ErrorPage(HttpStatus.NOT_FOUND, "/error404");
            ErrorPage errorPage500 = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500");

            factory.addErrorPages(errorPage401, errorPage404, errorPage500);
        };
    }

    /**
     * cookie对象，会话cookie模板，设置为jessionid问题，与servlet容器重新定义为sid或者rememberMe
     *
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
     * cookie 管理对象, 记住我功能
     *
     * @return
     */
    @Bean
    public CookieRememberMeManager rememberMeManager() {
        CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
        cookieRememberMeManager.setCookie(rememberMeCookie());
        cookieRememberMeManager.setCipherKey(Base64.decode("4AvVhmFLUs0KTA3Kprsdag=="));
        return cookieRememberMeManager;
    }


    /**
     * 过滤器 记住我
     *
     * @return
     */
    @Bean
    public FormAuthenticationFilter formAuthenticationFilter() {
        FormAuthenticationFilter formAuthenticationFilter = new FormAuthenticationFilter();
        //对应前端的记住我checkbox name=rememberMe
        formAuthenticationFilter.setRememberMeParam("rememberMe");
        return formAuthenticationFilter;
    }

    /**
     * shiro缓存管理器;
     * 需要添加到securityManager中
     *
     * @return
     */
    @Bean
    public EhCacheManager ehCacheManager() {
        EhCacheManager ehCacheManager = new EhCacheManager();
        ehCacheManager.setCacheManagerConfigFile("classpath:config/ehcache-shiro.xml");
        return ehCacheManager;
    }

    /**
     * 让某个实例的某个方法的返回值注入为Bean的实例
     * Spring静态注入
     *
     * @return
     */
    @Bean
    public MethodInvokingFactoryBean methodInvokingFactoryBean(@Qualifier("shiroRealm") ShiroRealm shiroRealm) {
        MethodInvokingFactoryBean factoryBean = new MethodInvokingFactoryBean();
        factoryBean.setStaticMethod("org.apache.shiro.SecurityUtils.setSecurityManager");
        factoryBean.setArguments(new Object[]{
                securityManager(shiroRealm)
        });

        return factoryBean;
    }


    /**
     * @return 配置session监听
     */
    @Bean
    public ShiroSessionListener sessionListener() {
        return new ShiroSessionListener();
    }

    /**
     * 配置会话Id生成器
     *
     * @return
     */
    @Bean
    public SessionIdGenerator sessionIdGenerator() {
        return new JavaUuidSessionIdGenerator();
    }

    /**
     * Session的作用是为session提供crud并进行持久化的一个shiro组件
     * Memory SessionDao直接在内润中进行会话维护
     * EnterpriseCacheSessionDao提供了缓存功能的会话维护,默认情况下使用MapCacha使用
     *
     * @return
     */
    @Bean
    public SessionDAO sessionDAO() {
        EnterpriseCacheSessionDAO enterpriseCacheSessionDAO = new EnterpriseCacheSessionDAO();
        //使用ehcacheManager
        enterpriseCacheSessionDAO.setCacheManager(ehCacheManager());
        enterpriseCacheSessionDAO.setActiveSessionsCacheName("shiro-activeSessionCache");
        //sessionId生成器
        enterpriseCacheSessionDAO.setSessionIdGenerator(sessionIdGenerator());
        return enterpriseCacheSessionDAO;
    }

    /**
     * 配置保存sessionId的cookie
     * 注意：这里
     *
     * @return
     */

    @Bean("sessionIdCookie")
    public SimpleCookie sessionIdCookie() {
        //cookie名称
        SimpleCookie simpleCookie = new SimpleCookie("sid");
        //设置为true表示只能http访问,javascript无法访问
        simpleCookie.setHttpOnly(true);
        simpleCookie.setPath("/");
        //-1表示关闭浏览器失效
        simpleCookie.setMaxAge(-1);
        return simpleCookie;
    }

    /**
     * 配置会话管理器,设定会话超时及保存
     *
     * @return
     */

    @Bean("sessionManager")
    public SessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        Collection<SessionListener> listeners = new ArrayList<>();
        listeners.add(sessionListener());
        sessionManager.setSessionIdCookie(sessionIdCookie());
        sessionManager.setSessionDAO(sessionDAO());
        sessionManager.setCacheManager(ehCacheManager());

        //全局会话超时时间（单位毫秒），默认30分钟  暂时设置为10秒钟 用来测试
        sessionManager.setGlobalSessionTimeout(10000);
        //是否开启删除无效的session对象  默认为true
        sessionManager.setDeleteInvalidSessions(true);
        //是否开启定时调度器进行检测过期session 默认为true
        sessionManager.setSessionValidationSchedulerEnabled(true);
        //设置session失效的扫描时间, 清理用户直接关闭浏览器造成的孤立会话 默认为 1个小时
        //设置该属性 就不需要设置 ExecutorServiceSessionValidationScheduler 底层也是默认自动调用ExecutorServiceSessionValidationScheduler
        //暂时设置为 5秒 用来测试

        sessionManager.setSessionValidationInterval(5000);
        //取消url后面的JessionId
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        return sessionManager;
    }
    //并发登录控制

    @Bean
    public KickoutSessionControlFilter kickoutSessionControlFilter() {

        KickoutSessionControlFilter kickoutSessionControlFilter=new KickoutSessionControlFilter();
        //用于根据会话Id获取会话进行踢出操作
        kickoutSessionControlFilter.setSessionManager(sessionManager());
        //使用cacheManager获取相应的cache来缓存用户登录的会话,用于保存用户-会话之间的关系
        kickoutSessionControlFilter.setCacheManager(ehCacheManager());
        //时候踢出后来的登录的用户,默认为false
        kickoutSessionControlFilter.setKickoutAfter(false);
        //同一个用户的最大会话数,
        kickoutSessionControlFilter.setMaxSession(1);
        //被踢出之后的重定向地址
        kickoutSessionControlFilter.setKickoutUrl("/login?kickout=1");
        return kickoutSessionControlFilter;
    }
}
