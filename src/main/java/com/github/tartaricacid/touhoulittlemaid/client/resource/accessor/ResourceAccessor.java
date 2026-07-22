package com.github.tartaricacid.touhoulittlemaid.client.resource.accessor;

import java.io.IOException;
import java.io.InputStream;

/**
 * 为自定义模型包提供统一的资源访问接口。路径相对于资源包根目录解析，
 * 例如 {@code assets/namespace/path}；目录与压缩包分别提供具体实现。
 */
public interface ResourceAccessor {
    /**
     * 打开指定资源路径对应的输入流（路径相对资源包根目录）。
     */
    InputStream open(String path) throws IOException;

    /**
     * 检查资源是否存在。
     */
    boolean exists(String path);
}
