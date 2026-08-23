package com.github.tartaricacid.touhoulittlemaid.ai.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * 按配置里的代理地址选代理。
 *
 * <p>⚠️ 形参收的是**已解析的读法**（{@code Supplier<String>}）而不是配置对象本身。
 * 三个调用点的代理地址分属两种所有权——{@code LLM_PROXY_ADDRESS} / {@code TTS_PROXY_ADDRESS}
 * 是实例级 AI 规则（须经 {@code ServerRuleConfig.get} 路由到 AI 店），
 * {@code STT_PROXY_ADDRESS} 是个人配置（直接 {@code .get()}）。
 * 若照收配置对象、在本类里统一 {@code .get()} 或统一走读口，两种所有权必有一种读错：
 * 前者会让 AI 规则抛「配置未加载」，后者会让个人配置静默退回默认值。</p>
 *
 * <p>把「怎么读」留在调用点，读点就天然正确，也才扫得到——读点藏在形参后面正是
 * 本仓库栽过的漏改形态。</p>
 */
public class ConfigProxySelector extends ProxySelector {
    private static final List<Proxy> NO_PROXY_LIST = List.of(Proxy.NO_PROXY);
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    private final Supplier<String> address;

    public ConfigProxySelector(Supplier<String> address) {
        this.address = address;
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException e) {
    }

    @Override
    public synchronized List<Proxy> select(URI uri) {
        String scheme = uri.getScheme().toLowerCase();
        if (HTTP.equals(scheme) || HTTPS.equals(scheme)) {
            String value = this.address.get();
            return getProxyFromConfig(value.trim());
        } else {
            return NO_PROXY_LIST;
        }
    }

    private synchronized List<Proxy> getProxyFromConfig(String proxyAddress) {
        if (StringUtils.isBlank(proxyAddress)) {
            return NO_PROXY_LIST;
        }
        String[] split = proxyAddress.split(":", 2);
        if (split.length != 2) {
            return NO_PROXY_LIST;
        }
        String hostname = split[0];
        String portString = split[1];
        if (!StringUtils.isNumeric(portString) || !NumberUtils.isParsable(portString)) {
            return NO_PROXY_LIST;
        }
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostname, Integer.parseInt(portString)));
        return List.of(proxy);
    }
}