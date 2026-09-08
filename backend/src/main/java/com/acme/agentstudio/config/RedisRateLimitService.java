package com.acme.agentstudio.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 分布式限流服务。
 * 结合 Redis Lua 脚本，将键自增（INCR）、首次过期时间设置（EXPIRE）与阈值判断整合为一个原子操作，
 * 避免并发竞争与过期时间未成功设置导致的死键问题。
 */
@Service
public class RedisRateLimitService {

    /** Redis 原子计数与限流 Lua 脚本 */
    private static final String RATE_LIMIT_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            if current > tonumber(ARGV[2]) then
                return 0
            end
            return 1
            """;

    /** Spring Data Redis 模版 */
    private final StringRedisTemplate redisTemplate;

    /** 预编译的 Redis Lua 脚本实例 */
    private final DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);

    /**
     * 构造函数注入 StringRedisTemplate。
     *
     * @param redisTemplate Redis 操作模版
     */
    public RedisRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 判断特定业务 Key 在指定滑动窗口跨度内是否允许继续访问。
     *
     * @param businessKey   业务标识（如 "用户ID:请求路径" 或 "IP地址"）
     * @param maxRequests   窗口内允许的最大请求次数
     * @param windowSeconds 观察窗口时长（秒）
     * @return 若未超出阈值允许通行则返回 true；否则返回 false
     */
    public boolean isAllowed(String businessKey, int maxRequests, int windowSeconds) {
        String redisKey = "agent-studio:rate-limit:" + businessKey;
        Long result = redisTemplate.execute(
                script,
                List.of(redisKey),
                String.valueOf(windowSeconds),
                String.valueOf(maxRequests)
        );
        return Long.valueOf(1L).equals(result);
    }
}

