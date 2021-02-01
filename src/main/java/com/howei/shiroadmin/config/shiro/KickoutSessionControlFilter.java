package com.howei.shiroadmin.config.shiro;

import com.howei.shiroadmin.model.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import sun.rmi.runtime.Log;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Deque;
import java.util.LinkedList;

@Slf4j
public class KickoutSessionControlFilter extends AccessControlFilter {

    /**
     * 踢出后的位置
     */
    private String KickoutUrl;
    /**
     * 提出之前的/之后登录的用户,默认踢出之前登录的用户
     */
    private boolean kickoutAfter = false;
    /**
     * 同一个账号最大会话数默认1
     */
    private int maxSession = 1;

    private SessionManager sessionManager;
    //方法一
//    private Cache<String, Deque<Serializable>> cache;
//    public Cache<String, Deque<Serializable>> getCacheManager() {
//        return cache;
//    }
//    public void setCacheManager(CacheManager cacheManager) {
//        this.cache = cacheManager.getCache("shiro-activeSessionCache");
//    }
    //方法二 redis缓存
    @Autowired
    private ResourceUrlProvider resourceUrlProvider;
    private RedisManager redisManager;
    private static final String DEFAULT_KICKOUT_CACHE_KEY_PREFIX = "shiro:cache:kickout:";
    private String keyPrefix = DEFAULT_KICKOUT_CACHE_KEY_PREFIX;


    public RedisManager getRedisManager() {
        return redisManager;
    }

    public void setRedisManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    public String getKickoutUrl() {
        return KickoutUrl;
    }

    public void setKickoutUrl(String kickoutUrl) {
        KickoutUrl = kickoutUrl;
    }

    public boolean isKickoutAfter() {
        return kickoutAfter;
    }

    public void setKickoutAfter(boolean kickoutAfter) {
        this.kickoutAfter = kickoutAfter;
    }

    public int getMaxSession() {
        return maxSession;
    }

    public void setMaxSession(int maxSession) {
        this.maxSession = maxSession;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    private String getRedisKickoutKey(String username) {
        return this.keyPrefix + username;
    }


    /**
     * 是否允许访问,true为允许
     *
     * @param servletRequest
     * @param servletResponse
     * @param o
     * @return
     * @throws Exception
     */
    @Override
    protected boolean isAccessAllowed(ServletRequest servletRequest, ServletResponse servletResponse, Object o) throws Exception {
        return false;
    }

    /**
     * 表示拒绝访问是是否自己处理,如果返回true表示自己不处理且继续拦截过滤器,返回false
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        Subject subject = getSubject(request, response);
        if (!subject.isAuthenticated() && !subject.isRemembered()) {
            //没有登录直接尽心之后的流程
            return true;
        }
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String path = httpServletRequest.getServletPath();

        if (isStaticFile(path)) {
            return true;
        }
        Session session = subject.getSession();
        //这里的获取的User是实体,因为在shiroRealm中的认证方法中,传的是User实体
        String username = ((User) subject.getPrincipal()).getUsername();
        Serializable sessionId = session.getId();
        //方法一:ehcache缓存
        // 初始化用户的队列放在缓存中
//        Deque<Serializable> deque = cache.get(username);
        //方法二:redis缓存
        Deque<Serializable> deque = (Deque<Serializable>) redisManager.get(getRedisKickoutKey(username));


        if (deque == null || deque.size() == 0) {
            deque = new LinkedList<>();
        }


        //如果队列中没有此sessionId却用户没有推出,放入队列
        if (!deque.contains(sessionId) && session.getAttribute("kickout") == null) {
            deque.push(sessionId);
        }
        //如果队列里的sessionId数超出最大会话数,开始踢人
        while (deque.size() > maxSession) {
            Serializable kickoutSessionId = null;
            //踢出先登录的用户或者后登录的用户,true为之前登录的用户
            if (kickoutAfter) {
                kickoutSessionId = deque.getFirst();
                kickoutSessionId = deque.removeFirst();
            } else {
                kickoutSessionId = deque.removeLast();
            }
            try {
                DefaultSessionKey defaultSessionKey = new DefaultSessionKey(kickoutSessionId);
                Session kickoutSession = sessionManager.getSession(defaultSessionKey);
                if (kickoutSession == null) {
                    kickoutSession.setAttribute("kickout", true);

                }
            } catch (Exception e) {
                log.error(e.toString());
            }

        }
        //如果被踢出了，直接退出，重定向到踢出后的地址
        if (session.getAttribute("kickout") != null) {
            try {
                subject.logout();
            } catch (Exception e) {

                log.error(e.toString());
            }
            WebUtils.issueRedirect(request, response, KickoutUrl);
            return false;
        }
        return true;

    }

    private boolean isStaticFile(String path) {
        String forLookupPath = resourceUrlProvider.getForLookupPath(path);
        return forLookupPath != null;
    }
}
