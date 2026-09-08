package com.acme.agentstudio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 数据库基线表结构与初始化数据加载器。
 * 实现 CommandLineRunner 接口，在应用启动完成前校验 `sql/schema.sql` 和 `sql/data.sql` 脚本，保障全新安装环境的零依赖初始化。
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    /** 数据源 */
    private final DataSource dataSource;

    /**
     * 构造函数注入数据源。
     *
     * @param dataSource 数据源
     */
    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 启动回调逻辑。
     *
     * @param args 命令行参数
     */
    @Override
    public void run(String... args) {
        // 说明：目前已改由开发人员显式执行 SQL 脚本建表与初始化，避免在生产环境启动时误覆盖表结构。
    }

    /**
     * 检查租户表 `tenant` 是否为空，判断是否需要填充基线种子数据。
     */
    private boolean isTenantTableEmpty() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM tenant");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() && resultSet.getLong(1) == 0;
        }
    }

    /**
     * 依次从相对文件系统、backend 子目录以及 Classpath 路径解析定位 SQL 脚本文件。
     */
    private Resource resolveResource(String relativePath) {
        Resource resource = new FileSystemResource(relativePath);
        if (!resource.exists()) {
            resource = new FileSystemResource("backend/" + relativePath);
        }
        if (!resource.exists()) {
            resource = new ClassPathResource(relativePath);
        }
        return resource;
    }
}

