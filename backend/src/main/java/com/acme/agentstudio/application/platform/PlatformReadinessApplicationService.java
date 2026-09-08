package com.acme.agentstudio.application.platform;

import com.acme.agentstudio.application.runtime.RuntimeStrategyRegistry;
import com.acme.agentstudio.application.task.PersistentTaskWorker;
import com.acme.agentstudio.domain.knowledge.port.VectorIndexProvider;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformToolConnectorEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysModelConfigEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformToolConnectorMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysModelConfigMapper;
import com.acme.agentstudio.infrastructure.workflow.NodeRegistry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台启动与运行就绪自检（Platform Readiness Check）服务。
 * 提供零入侵的诊断端点，覆盖 MySQL 数据库、Redis 缓存、后台 Task Worker 消费者、工作流节点目录、Runtime 策略引擎、大模型配置、工具连接器及 Qdrant 向量数据库，并输出前端可直接渲染的诊断摘要。
 */
@Service
public class PlatformReadinessApplicationService {

    /** 状态：已就绪 */
    private static final String READY = "READY";

    /** 状态：降级运行 */
    private static final String DEGRADED = "DEGRADED";

    /** 状态：不可用 */
    private static final String UNAVAILABLE = "UNAVAILABLE";

    /** 状态标识：活动中 */
    private static final String ACTIVE_STATUS = "ACTIVE";

    /** 数据库探测 SQL 语句 */
    private static final String DATABASE_PROBE_SQL = "SELECT 1";

    /** 关系型数据库数据源 */
    private final DataSource dataSource;

    /** Redis 操作模板 */
    private final StringRedisTemplate redisTemplate;

    /** 大模型配置 Persistence Mapper */
    private final SysModelConfigMapper modelConfigMapper;

    /** 工具连接器 Persistence Mapper */
    private final PlatformToolConnectorMapper connectorMapper;

    /** 工作流节点注册表 */
    private final NodeRegistry nodeRegistry;

    /** Runtime 策略注册表 */
    private final RuntimeStrategyRegistry strategyRegistry;

    /** 持久化任务 Worker 消费者 */
    private final PersistentTaskWorker taskWorker;

    /** 向量索引服务 Provider */
    private final VectorIndexProvider vectorIndexProvider;

    /**
     * 构造函数注入各关键组件依赖。
     */
    public PlatformReadinessApplicationService(DataSource dataSource,
                                               StringRedisTemplate redisTemplate,
                                               SysModelConfigMapper modelConfigMapper,
                                               PlatformToolConnectorMapper connectorMapper,
                                               NodeRegistry nodeRegistry,
                                               RuntimeStrategyRegistry strategyRegistry,
                                               PersistentTaskWorker taskWorker,
                                               VectorIndexProvider vectorIndexProvider) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
        this.modelConfigMapper = modelConfigMapper;
        this.connectorMapper = connectorMapper;
        this.nodeRegistry = nodeRegistry;
        this.strategyRegistry = strategyRegistry;
        this.taskWorker = taskWorker;
        this.vectorIndexProvider = vectorIndexProvider;
    }

    /**
     * 执行全平台多维度就绪性探测（Database, Redis, Worker, NodeRegistry, Runtime, Models, Connectors, VectorStore）。
     *
     * @return 前端可直接渲染展示的就绪报告字典
     */
    public Map<String, Object> check() {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(checkDatabase());
        checks.add(checkRedis());
        checks.add(checkWorker());
        checks.add(checkNodeRegistry());
        checks.add(checkRuntimeStrategies());
        checks.add(checkModels());
        checks.add(checkConnectors());
        checks.add(checkRagStore());

        long unavailableCount = checks.stream()
                .filter(item -> UNAVAILABLE.equals(item.get("status")))
                .count();

        long degradedCount = checks.stream()
                .filter(item -> DEGRADED.equals(item.get("status")))
                .count();

        String overallStatus = unavailableCount > 0
                ? UNAVAILABLE
                : (degradedCount > 0 ? DEGRADED : READY);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", overallStatus);
        result.put("checkedAt", Instant.now().toString());
        result.put("checks", checks);
        result.put("summary", Map.of(
                "total", checks.size(),
                "unavailable", unavailableCount,
                "degraded", degradedCount
        ));
        return result;
    }

    /**
     * 探测 MySQL 关系型数据库连通性。
     */
    private Map<String, Object> checkDatabase() {
        long startedAt = System.nanoTime();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(DATABASE_PROBE_SQL)) {
            statement.executeQuery();
            return ready("database", "数据库", "数据库连接正常", startedAt, Map.of());
        } catch (Exception exception) {
            return failed("database", "数据库", "数据库连接失败，请检查数据库连接地址与账号密码", startedAt, exception);
        }
    }

    /**
     * 探测 Redis 缓存服务连通性与 PONG 响应。
     */
    private Map<String, Object> checkRedis() {
        long startedAt = System.nanoTime();
        RedisConnection connection = null;
        try {
            connection = redisTemplate.getConnectionFactory().getConnection();
            String response = connection.ping();
            if (!"PONG".equalsIgnoreCase(response)) {
                return degraded("redis", "缓存服务", "缓存服务未正常返回 PONG 响应，平台将无法稳定执行限流与分布式锁协调", startedAt,
                        Map.of("response", response == null ? "" : response));
            }
            return ready("redis", "缓存服务", "缓存连接正常", startedAt, Map.of());
        } catch (Exception exception) {
            return failed("redis", "缓存服务", "缓存服务连接失败，请检查 Redis 配置", startedAt, exception);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * 检查后台异步 Task Worker 消费者的启动就绪状态。
     */
    private Map<String, Object> checkWorker() {
        long startedAt = System.nanoTime();
        if (taskWorker.isReady()) {
            return ready("worker", "任务 Worker", "异步任务消费者已就绪启动", startedAt, Map.of());
        }
        return unavailable("worker", "任务 Worker", "异步任务消费者未就绪，应用发布后的后台任务无法入队执行", startedAt, Map.of());
    }

    /**
     * 检查工作流节点目录（NodeRegistry）的加载就绪状态。
     */
    private Map<String, Object> checkNodeRegistry() {
        long startedAt = System.nanoTime();
        try {
            int count = nodeRegistry.list().size();
            if (count == 0) {
                return unavailable("node-registry", "节点目录", "工作流节点目录为空，无法创建可执行的流程图", startedAt, Map.of());
            }
            return ready("node-registry", "节点目录", "工作流节点目录已成功加载", startedAt, Map.of("nodeCount", count));
        } catch (Exception exception) {
            return failed("node-registry", "节点目录", "节点目录加载失败，请检查节点配置", startedAt, exception);
        }
    }

    /**
     * 检查 Runtime 策略引擎对多运行模式的支持加载情况。
     */
    private Map<String, Object> checkRuntimeStrategies() {
        long startedAt = System.nanoTime();
        long enabledCount = Arrays.stream(RuntimeMode.values())
                .filter(strategyRegistry::supports)
                .count();
        if (enabledCount == 0) {
            return unavailable("runtime-strategy", "运行策略", "没有可用的 Runtime 策略引擎，应用无法执行", startedAt, Map.of());
        }
        return ready("runtime-strategy", "运行策略", "Runtime 策略引擎已成功加载", startedAt, Map.of("enabledCount", enabledCount));
    }

    /**
     * 检查平台的大模型配置就绪状态。
     */
    private Map<String, Object> checkModels() {
        long startedAt = System.nanoTime();
        try {
            long count = modelConfigMapper.selectCount(new LambdaQueryWrapper<SysModelConfigEntity>()
                    .eq(SysModelConfigEntity::getStatus, ACTIVE_STATUS));
            if (count == 0) {
                return degraded("models", "模型服务", "暂无处于活动状态的模型配置，应用可编排但无法完成模型节点调度", startedAt, Map.of("activeCount", 0));
            }
            return ready("models", "模型服务", "已发现已启用的模型配置", startedAt, Map.of("activeCount", count));
        } catch (Exception exception) {
            return failed("models", "模型服务", "模型配置读取失败，请检查模型中心配置", startedAt, exception);
        }
    }

    /**
     * 检查工具连接器目录的可用性。
     */
    private Map<String, Object> checkConnectors() {
        long startedAt = System.nanoTime();
        try {
            long count = connectorMapper.selectCount(new LambdaQueryWrapper<PlatformToolConnectorEntity>()
                    .eq(PlatformToolConnectorEntity::getStatus, ACTIVE_STATUS));
            return ready("connectors", "工具连接器", "工具连接器目录读取正常", startedAt, Map.of("activeCount", count));
        } catch (Exception exception) {
            return failed("connectors", "工具连接器", "连接器目录读取失败，请检查工具配置", startedAt, exception);
        }
    }

    /**
     * 检查 Qdrant 向量索引数据库连通性。
     */
    private Map<String, Object> checkRagStore() {
        long startedAt = System.nanoTime();
        try {
            vectorIndexProvider.checkHealth();
            return ready("rag", "知识检索", "Qdrant 向量索引数据库连接正常", startedAt, Map.of());
        } catch (RuntimeException exception) {
            return failed("rag", "知识检索", "Qdrant 向量索引服务不可用，请检查服务配置", startedAt, exception);
        }
    }

    /** 构造成功的 CheckResult */
    private Map<String, Object> ready(String key, String label, String message, long startedAt, Map<String, Object> details) {
        return item(key, label, READY, message, startedAt, details);
    }

    /** 构造降级的 CheckResult */
    private Map<String, Object> degraded(String key, String label, String message, long startedAt, Map<String, Object> details) {
        return item(key, label, DEGRADED, message, startedAt, details);
    }

    /** 构造不可用的 CheckResult */
    private Map<String, Object> unavailable(String key, String label, String message, long startedAt, Map<String, Object> details) {
        return item(key, label, UNAVAILABLE, message, startedAt, details);
    }

    /** 构造失败的 CheckResult */
    private Map<String, Object> failed(String key, String label, String message, long startedAt, Exception exception) {
        return item(key, label, UNAVAILABLE, message, startedAt, Map.of("errorType", exception.getClass().getSimpleName()));
    }

    /** 拼接 Check 节点的数据结构 */
    private Map<String, Object> item(String key, String label, String status, String message, long startedAt,
                                     Map<String, Object> details) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("label", label);
        item.put("status", status);
        item.put("message", message);
        item.put("latencyMs", (System.nanoTime() - startedAt) / 1_000_000);
        item.put("details", details);
        return item;
    }
}

