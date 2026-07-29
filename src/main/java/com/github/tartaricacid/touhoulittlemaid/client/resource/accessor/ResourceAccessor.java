package com.github.tartaricacid.touhoulittlemaid.client.resource.accessor;

import java.io.IOException;
import java.io.InputStream;

/**
 * 为自定义资源包资源提供抽象访问接口。
 * 路径相对于资源包根目录解析（例如：assets/namespace/path）。
 * <p>Phase 3 Batch 3b keystone：FileResourceAccessor / ZipResourceAccessor 实现它。
 */
public interface ResourceAccessor {
    /** 打开指定资源路径对应的输入流（路径相对资源包根目录）。 */
    InputStream open(String path) throws IOException;

    /** 检查资源是否存在。 */
    boolean exists(String path);
}
