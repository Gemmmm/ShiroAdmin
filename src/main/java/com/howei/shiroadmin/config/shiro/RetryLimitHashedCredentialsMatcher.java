package com.howei.shiroadmin.config.shiro;

import com.howei.shiroadmin.model.User;
import com.howei.shiroadmin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登陆次数限制
 * 这里就不对密码进行加密,如果有需要加密,将自定义密码比较器从SimpleCredentialsMatcher
 * 改为HashedCredentialsMatcher,然后将对应的配置项打开就可以。
 *
 * @author jayun
 */
@Slf4j
public class RetryLimitHashedCredentialsMatcher extends SimpleCredentialsMatcher {

    @Autowired
    private UserService userService;

    private Cache<String, AtomicInteger> passwordRetryCache;

    public RetryLimitHashedCredentialsMatcher(CacheManager cacheManager) {
        this.passwordRetryCache = cacheManager.getCache("passwordRetryCache");
    }

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        log.info("登录失败处理");
        //获取用户名
        String username = (String) token.getPrincipal();
        //获取用户登录次数
        AtomicInteger retryCount = passwordRetryCache.get(username);
        if (retryCount == null) {
            //弱国用户没有登录过,登录次数jiayi,并放入缓存
            retryCount = new AtomicInteger(0);
            passwordRetryCache.put(username, retryCount);
        }
        if (retryCount.incrementAndGet() > 5) {
            User user = userService.getByUsername(username);
            if (user != null && "0".equals(user.getState())) {
                user.setState("1");
                int count = userService.update(user);
            }
            log.info("锁定账户" + user.getUsername());
            throw new LockedAccountException();
        }
        boolean match = super.doCredentialsMatch(token, info);
        if (match) {
            //如果正确,从缓存中国将用户登录计数清除
            passwordRetryCache.remove(username);
        }
        return match;
    }

    /**
     * 根据用户名解锁用户
     *
     * @param username
     */
    public void unlockAccount(String username) {
        User user = userService.getByUsername(username);
        if (user != null) {
            user.setState("0");
            userService.update(user);
            passwordRetryCache.remove(username);
        }
    }
}

