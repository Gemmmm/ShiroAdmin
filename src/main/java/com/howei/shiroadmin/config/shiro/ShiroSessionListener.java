package com.howei.shiroadmin.config.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jayun
 * 配置session监听
 */
public class ShiroSessionListener implements SessionListener {

    /**
     * 统计在线人数
     * juc包下线程安全自增
     */
    private final AtomicInteger sessionCount = new AtomicInteger(0);


    /**
     * 会话创建是触发
     *
     * @param session
     */
    @Override
    public void onStart(Session session) {
        //会话创建,在线人数加一
        sessionCount.incrementAndGet();
    }

    /**
     * 退出时触发
     * @param session
     */
    @Override
    public void onStop(Session session) {
        //会话退出,在线人数减一
        sessionCount.decrementAndGet();

    }

    /**
     * 会话过期时触发
     * @param session
     */
    @Override
    public void onExpiration(Session session) {
        //会话过期,在线人数减一
        sessionCount.decrementAndGet();
    }

    /**
     *
     * @return 获取在线人数
     */
    public AtomicInteger getSessionCount(){
        return sessionCount;
    }
}
