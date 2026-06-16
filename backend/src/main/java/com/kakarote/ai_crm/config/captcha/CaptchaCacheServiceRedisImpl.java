package com.kakarote.ai_crm.config.captcha;

import com.anji.captcha.service.CaptchaCacheService;
import com.kakarote.ai_crm.common.redis.Redis;

/**
 * 验证码redis缓存实现
 *
 * @author zhangzhiwei
 * @date 2022-05-12
 */
public class CaptchaCacheServiceRedisImpl implements CaptchaCacheService {

    private Redis redis;

    /**
     * 处理type方法逻辑。
     */
    @Override
    public String type() {
        return "redis";
    }

    /**
     * 设置Redis。
     */
    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    /**
     * 设置验证码缓存ServiceRedisImpl。
     */
    @Override
    public void set(String key, String value, long expiresInSeconds) {
        redis.setex(key, ((Long) expiresInSeconds).intValue(), value);
    }

    /**
     * 处理exists方法逻辑。
     */
    @Override
    public boolean exists(String key) {
        return redis.exists(key);
    }

    /**
     * 删除验证码缓存ServiceRedisImpl。
     */
    @Override
    public void delete(String key) {
        redis.del(key);
    }

    /**
     * 获取验证码缓存ServiceRedisImpl。
     */
    @Override
    public String get(String key) {
        return redis.get(key);
    }

    /**
     * 处理increment方法逻辑。
     */
    @Override
    public Long increment(String key, long val) {
        return redis.incr(key, val);
    }
}
