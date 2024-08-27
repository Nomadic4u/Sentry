package org.example.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 限流通用工具
 * 针对于不同的情况进行限流操作, 支持限流升级
 */
@Slf4j
@Component
public class FlowUtils {

    @Resource
    StringRedisTemplate template;


    /**
     * 针对于单次频率限制, 请求成功后, 冷却时间内不能再吃进行请求, 如3s内不能再发起请求
     * 如果redis中已经含有这个键了, 说明在重复访问
     *
     * @param key       键
     * @param blockTime 限制时间
     * @return 是否通过限流检查
     */
    public boolean limitOnceCheck(String key, int blockTime) {
        return this.internalCheck(key, 1, blockTime, (overclock) -> false);
    }

    /**
     * 若不听劝阻继续发起请求, 则限制更长的时间
     *
     * @param key         键
     * @param frequency   请求频率
     * @param baseTime    基础限制时间
     * @param upgradeTime 升级限制时间
     * @return 是否通过限流检查
     */
    public boolean limitOnceUpdateCheck(String key, int frequency, int baseTime, int upgradeTime) {
        return this.internalCheck(key, frequency, baseTime, (overclock) -> {
            if (overclock)
                template.opsForValue().set(key, "1", upgradeTime, TimeUnit.SECONDS);
            return false;
        });
    }

    /**
     * 值得一提的是 这个是先为internalCheck提供接口的实现类, 再由internalCheck传入参数供调用
     *
     * @param counterKey 计数键
     * @param blockKey   封禁键
     * @param blockTime  封禁时间
     * @param frequency  请求频率
     * @param period     计数周期
     * @return 是否通过限流检查
     */
    public boolean limitPeriodCheck(String counterKey, String blockKey, int blockTime, int frequency, int period) {
        return this.internalCheck(counterKey, frequency, period, (overclock) -> {
            if (overclock)
                template.opsForValue().set(blockKey, "", blockTime, TimeUnit.SECONDS);
            return !overclock;
        });
    }

    /**
     * 内部使用请求限制的主要逻辑
     *
     * @param key       计键器
     * @param frequency 请求频率
     * @param period    计数周期
     * @param action    限制行为与策略
     * @return 是否通过限流检查
     */
    private boolean internalCheck(String key, int frequency, int period, LimitAction action) {
        if (Boolean.TRUE.equals(template.hasKey(key))) {
            Long value = Optional.ofNullable(template.opsForValue().increment(key)).orElse(0L);
            return action.run(value > frequency);
        } else {
            template.opsForValue().set(key, "1", period, TimeUnit.SECONDS);
            return true;
        }
    }

    /**
     * 内部使用, 限制行为与策略
     */
    private interface LimitAction {
        boolean run(boolean overclock);
    }
}
