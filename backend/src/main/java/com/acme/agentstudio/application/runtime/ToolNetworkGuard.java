package com.acme.agentstudio.application.runtime;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * 运行时工具 HTTP/HTTPS 外设网络安全出口守卫（Tool Network Guard）。
 * 防范 SSRF（服务端请求伪造）攻击：
 * 校验工具调用的目标 URL 地址，强校验协议（只允许 http 与 https）、禁止 URI 携带明文用户信息（userInfo），
 * 解析域名 IP 强行拦截与阻断回环地址（isLoopbackAddress）、内网私有地址（isSiteLocalAddress）、链路本地地址（isLinkLocalAddress）及任意本地绑定地址（isAnyLocalAddress）。
 */
@Component
public class ToolNetworkGuard {

    /** HTTP 协议 Scheme */
    private static final String HTTP_SCHEME = "http";

    /** HTTPS 协议 Scheme */
    private static final String HTTPS_SCHEME = "https";

    /**
     * 强校验工具目标网络 URL 地址是否安全可信，不安全时抛出 IllegalArgumentException 阻断请求。
     *
     * @param targetUri 目标 URL 字符串
     */
    public void requireSafeTarget(String targetUri) {
        if (targetUri == null || targetUri.isBlank()) {
            return;
        }

        URI uri;
        try {
            uri = URI.create(targetUri);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("工具调用的目标网络地址格式无效：" + targetUri, exception);
        }

        if (!HTTP_SCHEME.equalsIgnoreCase(uri.getScheme()) && !HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("出于安全合规要求，工具只允许访问 HTTP 或 HTTPS 协议地址，禁止使用 [" + uri.getScheme() + "] 协议。");
        }

        if (uri.getUserInfo() != null || uri.getHost() == null) {
            throw new IllegalArgumentException("工具目标网络地址不允许在 URL 中携带用户信息（UserInfo），且必须包含有效主机名 Host。");
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
                    throw new IllegalArgumentException("安全防护拦截：工具目标网络地址 [" + uri.getHost() + "] (" + address.getHostAddress() + ") 指向受禁止的内网私有地址或本机回环地址，已被系统阻断。");
                }
            }
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("工具目标网络地址主机名 [" + uri.getHost() + "] 无法解析为有效 IP 地址。", exception);
        }
    }
}

