package com.kakarote.ai_crm.config.captcha;

import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.kakarote.ai_crm.common.redis.Redis;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储策略自动配置.
 */
@Configuration
public class AjCaptchaStorageAutoConfiguration {

    /**
     * 处理captchaCacheService方法逻辑。
     */
    @Bean(name = "AjCaptchaCacheService")
    public CaptchaCacheService captchaCacheService(AjCaptchaProperties ajCaptchaProperties, Redis redis) {
        //缓存类型redis/local/....
        CaptchaCacheService cacheService = CaptchaServiceFactory.getCache(ajCaptchaProperties.getCacheType().name());
        if (cacheService instanceof CaptchaCacheServiceRedisImpl redisCacheService) {
            redisCacheService.setRedis(redis);
        }
        return cacheService;
    }
}
