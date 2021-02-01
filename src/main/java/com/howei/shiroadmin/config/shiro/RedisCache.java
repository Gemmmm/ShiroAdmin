package com.howei.shiroadmin.config.shiro;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.CollectionUtils;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.exception.PrincipalIdNullException;
import org.crazycake.shiro.exception.PrincipalInstanceException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Slf4j
public class RedisCache<K, V> implements Cache<K, V> {

    private RedisManager redisManager;
    private String keyPrefix = "";
    private int expire = 0;
    private String principalIdFieldName = RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME;

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getPrincipalIdFieldName() {
        return principalIdFieldName;
    }

    public void setPrincipalIdFieldName(String principalIdFieldName) {
        this.principalIdFieldName = principalIdFieldName;
    }

    public RedisCache(RedisManager redisManager, String keyPrefix, int expire, String principalIdFieldName) {

        if (redisManager == null) {
            throw new IllegalArgumentException("redisManager cannot be null");
        }
        this.redisManager = redisManager;
        if (keyPrefix != null && !"".equals(keyPrefix)) {
            this.keyPrefix = keyPrefix;
        }
        if (expire != -1) {

            this.expire = expire;
        }
        if (principalIdFieldName != null && !"".equals(principalIdFieldName)) {
            this.principalIdFieldName = principalIdFieldName;
        }
    }

    @Override
    public V get(K key) throws CacheException {
        log.debug("get key [{}]", key);
        if (key == null) {
            return null;
        }
        try {
            String redisCacheKey = getRedisCacheKey(key);
            Object rawValue = redisManager.get(redisCacheKey);
            if (rawValue == null) {
                return null;
            }
            V value = (V) rawValue;
            return value;
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    @Override
    public V put(K key, V value) throws CacheException {
        log.debug("put key [{}]", key);
        if (key == null) {
            log.warn("Saving a null key is meaningless, return value directly without call Redis");
            return value;
        }
        try {
            String redisCacheKey = getRedisCacheKey(key);

            redisManager.set(redisCacheKey, value != null ? value : null, expire);
            return value;
        } catch (Exception e) {
            CacheException cacheException = new CacheException(e);
            throw cacheException;
        }

    }


    @Override
    public V remove(K key) throws CacheException {
        log.debug("remove key [{}]", key);
        if (key == null) {
            return null;
        }
        try {
            String redisCacheKey = getRedisCacheKey(key);
            Object rawValue = redisManager.get(redisCacheKey);
            V privios = (V) rawValue;
            redisManager.del(redisCacheKey);
            return privios;
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    @Override
    public void clear() throws CacheException {
        log.debug("clear chache");
        Set<String> keys = null;
        try {
            keys = redisManager.scan(this.keyPrefix + "*");

        } catch (Exception e) {
            log.error("get keys error", e);
        }
        if (keys == null || keys.size() == 0) {
            return;
        }
        for (String key : keys) {
            redisManager.del(key);
        }
    }

    @Override
    public int size() {

        Long longSize = 0L;
        try {
            longSize = new Long(redisManager.scanSize(this.keyPrefix + "*"));
        } catch (Exception e) {

            log.error("get keys error", e);
        }
        return longSize.intValue();
    }

    @Override
    public Set<K> keys() {
        Set<String> keys = null;
        try {
            keys = redisManager.scan(this.keyPrefix + "*");
        } catch (Exception e) {

            log.error("get keys error", e);
            return Collections.emptySet();
        }
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptySet();
        }
        Set<K> convertedKeys = new HashSet<>();
        try {
            for (String key : keys) {
                convertedKeys.add((K) key);
            }
        } catch (Exception e) {
            log.error("deserialize keys error", e);
        }

        return convertedKeys;
    }

    @Override
    public Collection<V> values() {
        Set<String> keys = null;
        try {
            keys = redisManager.scan(this.keyPrefix + "*");
        } catch (Exception e) {
            log.error("get values error ", e);
            return Collections.emptySet();
        }
        List<V> values = new ArrayList<>();
        for (String key : keys) {
            V value = null;
            try {
                value = (V) redisManager.get(key);
            } catch (Exception e) {

                log.error("deserialize values error", e);
            }
            if (value != null) ;
            {
                values.add(value);
            }

        }
        return null;
    }

    private String getRedisCacheKey(K key) {
        if (key == null) {
            return null;
        }
        return this.keyPrefix + getStringRedisKey(key);
    }

    private String getStringRedisKey(K key) {
        String redisKey;
        if (key instanceof PrincipalCollection) {
            redisKey = getRedisKeyFromPrincipalIdField((PrincipalCollection) key);
        } else {
            redisKey = key.toString();
        }
        return redisKey;
    }

    private String getRedisKeyFromPrincipalIdField(PrincipalCollection key) {
        String redisKey;
        Object principalObject = key.getPrimaryPrincipal();
        Method principalIdGetter = null;
        Method[] methods = principalObject.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME.equalsIgnoreCase(this.principalIdFieldName) && ("getAuthCacheKey".equals(m.getName()) || "getId".equals(m.getName()))) {
                principalIdGetter = m;
                break;
            }
            if (m.getName().equals("get" + this.principalIdFieldName.substring(0, 1).toUpperCase() + this.principalIdFieldName.substring(1))) {
                principalIdGetter = m;
                break;
            }
        }

        if (principalIdGetter == null) {
            throw new PrincipalInstanceException(principalObject.getClass(), this.principalIdFieldName);

        }

        try {

            Object idObj = principalIdGetter.invoke(principalObject);
            if (idObj == null) {
                throw new PrincipalIdNullException(principalObject.getClass(), this.principalIdFieldName);
            }
            redisKey = idObj.toString();
        } catch (IllegalAccessException e) {
            throw new PrincipalInstanceException(principalObject.getClass(), this.principalIdFieldName, e);
        } catch (InvocationTargetException e) {
            throw new PrincipalInstanceException(principalObject.getClass(), this.principalIdFieldName, e);
        }
        return redisKey;
    }
}
