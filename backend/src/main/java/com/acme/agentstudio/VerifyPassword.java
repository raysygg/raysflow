package com.acme.agentstudio;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 本地 BCrypt 密码 Hash 哈希校验与生成辅助工具类（Verify Password）。
 * 独立主程序入口，仅用于初始化或调试时验证 BCryptPasswordEncoder 密码算法。
 */
public class VerifyPassword {

    /**
     * 主函数入口：进行密码加密 Hash 生成与匹配校验测试。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$12$8lSK5HwV6mVv4fCpV5wmPukSN.3GHgZIoclAtTciSsvjioMJGULi6";

        String newHash = encoder.encode("LSY.admin123");
        System.out.println("GENERATED_HASH_OF_admin123:" + newHash);
        System.out.println("MATCH_TEST:" + encoder.matches("LSY.admin123", newHash));
        System.out.println("MATCH_TEST_SEED_HASH:" + encoder.matches("LSY.admin123", hash));
    }
}

