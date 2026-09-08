package com.acme.agentstudio.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/**
 * 系统内部唯一标识符生成工具类（System Identifier Generator）。
 * 用于根据用户输入的业务名称自动生成规格化的系统 slug 编码（包含 8 位随机 UUID 校验尾缀），
 * 确保用户只需关注可读的业务名称，系统内部自动维持规格化编码。
 */
public final class SystemIdentifierGenerator {

    /** 私有构造方法，防止工具类被实例化。 */
    private SystemIdentifierGenerator() {
    }

    /**
     * 根据业务名称生成简短且唯一的系统标识字符串。
     *
     * @param name 业务名称（支持中文、英文、数字等）
     * @param fallback 当业务名称无法提取出有效字符时的保底默认标识
     * @return 格式化后的系统标识符字符串
     */
    public static String fromName(String name, String fallback) {
        String input = (name == null) ? "" : name;
        String value = Normalizer.normalize(input, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (value.isBlank()) {
            value = fallback;
        }

        String uuidSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return value + "-" + uuidSuffix;
    }
}


