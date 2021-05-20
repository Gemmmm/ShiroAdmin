package com.howei.shiroadmin.config.shiro;


import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.ValidatingSession;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.apache.shiro.web.servlet.ShiroHttpSession;
import org.crazycake.shiro.SessionInMemory;

import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.*;

@Slf4j
public class RedisSessionDao extends AbstractSessionDAO {

    private static final String DEFAULT_SESSION_KEY_PREFIX = "shiro:session:";
    private String keyPrefix = DEFAULT_SESSION_KEY_PREFIX;
    private static final long DEFAULT_SESSION_IN_MEMORY_TIMEOUT = 1000L;
    private long sessionInmemoryTimeout = DEFAULT_SESSION_IN_MEMORY_TIMEOUT;
    private static final int DEFAULT_EXPIRE = -2;
    private static final int NO_EXPIRE = -1;
    private int expire = DEFAULT_EXPIRE;
    private static final int MILLISECONDS_IN_A_SECOND = 1000;
    private RedisManager redisManager;
    private static ThreadLocal sessionsInThread = new ThreadLocal();

    @Override
    public void update(Session session) throws UnknownSessionException {
        //如果会话过期，则不更新
        try {

            if (session instanceof ValidatingSession && !((ValidatingSession) session).isValid()) {
                return;
            }
            if(session instanceof ShiroSession){
                ShiroSession ss= (ShiroSession) session;
                //session除LastAccessTime上次访问时间以外的其他值没有变化
                if(!ss.isChanged()){
                    return;
                }
                //如果没有return,证明有调用setAttrivute向redis放的时候永远设置为false
                ss.setChanged(false);
            }
            this.saveSession(session);
        } catch (Exception e) {
            log.warn("update session is faild", e);

        }
    }

    private void saveSession(Session session) throws UnknownSessionException {
        if (session == null || session.getId() == null) {
            log.error("session or session id is null");
            throw new UnknownSessionException("session or sessionId is null");
        }
        String key = getRedisSessionKey(session.getId());
        if (expire == DEFAULT_EXPIRE) {
            this.redisManager.set(key, session, (int) (session.getTimeout()));
            return;
        }
        if (expire != NO_EXPIRE && expire * MILLISECONDS_IN_A_SECOND < session.getTimeout()) {
            log.warn("Redis session expire time: "
                    + (expire * MILLISECONDS_IN_A_SECOND)
                    + " is less than Session timeout: "
                    + session.getTimeout()
                    + " . It may cause some problems.");
        }
        this.redisManager.set(key, session, expire);

    }


    @Override
    public void delete(Session session) {
        if (session == null || session.getId() == null) {
            log.error("session or sessionId is null");
            return;
        }
        try {
            redisManager.del(getRedisSessionKey(session.getId()));
        } catch (Exception e) {

            log.error("delete session error,sessionId is {}", session.getId());
        }

    }

    @Override
    public Collection<Session> getActiveSessions() {
        Set<Session> sessions = new HashSet<Session>();
        try {
            Set<String> keys = redisManager.scan(this.keyPrefix + "*");
            if (keys != null && keys.size() > 0) {
                for (String key : keys) {
                    Session session = (Session) redisManager.get(key);
                    sessions.add(session);
                }
            }
        } catch (Exception e) {

            log.error("get active sessions error");
        }
        return sessions;
    }
    @Override
    protected Serializable doCreate(Session session) {
        if(session==null){
            log.error("session is null");
            throw new UnknownSessionException("session is null");

        }
        Serializable sessionId = this.generateSessionId(session);
        this.assignSessionId(session,sessionId);
        this.saveSession(session);
        return sessionId;
    }


    @Override
    protected Session doReadSession(Serializable sessionId) {
        if(sessionId==null){
            log.warn("sessionId is null");
            return null;
        }
        Session s=getSessionFromThreadLocal(sessionId);
        if(s!=null){
            return s;
        }
        log.debug("read session from redis");
        try {
            s= (Session) redisManager.get(getRedisSessionKey(sessionId));
            setSessionToThreadLocal(sessionId,s);

        } catch (Exception e) {
            log.error("read session error ,sessionId ="+sessionId);
        }
        return s;
    }

    private void setSessionToThreadLocal(Serializable sessionId, Session s) {
        Map<Serializable, SessionInMemory> sessionMap= (Map<Serializable, SessionInMemory>) sessionsInThread.get();
        if(sessionMap==null){
            sessionMap=new HashMap<>();
            sessionsInThread.set(sessionMap);
        }
        SessionInMemory sessionInMemory=new SessionInMemory();
        sessionInMemory.setCreateTime(new Date());
        sessionInMemory.setSession(s);
        sessionMap.put(sessionId,sessionInMemory);
    }

    private Session getSessionFromThreadLocal(Serializable sessionId) {
        Session s=null;
        if(sessionsInThread.get()==null){
            return null;
        }
        Map<Serializable,SessionInMemory> sessionMap=(Map<Serializable, SessionInMemory>) sessionsInThread.get();
        SessionInMemory sessionInMemory = sessionMap.get(sessionId);
        if(sessionInMemory==null){
            return null;
        }
        Date now=new Date();
        long duration=now.getTime()-sessionInMemory.getCreateTime().getTime();

        if(duration<sessionInmemoryTimeout){
            s=sessionInMemory.getSession();
            log.debug("read session from memory");
        }else{
            sessionMap.remove(sessionId);

        }

        return s;
    }


    private String getRedisSessionKey(Serializable sessionId) {
        return this.keyPrefix + sessionId;
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public void setRedisManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public long getSessionInmemoryTimeout() {
        return sessionInmemoryTimeout;
    }

    public void setSessionInmemoryTimeout(long sessionInmemoryTimeout) {
        this.sessionInmemoryTimeout = sessionInmemoryTimeout;
    }

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }


}


