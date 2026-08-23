package com.github.tartaricacid.touhoulittlemaid.ai.service.stt;

import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.aliyun.STTAliyunSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.siliconflow.STTSiliconflowSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.tencent.STTTencentSite;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「服务器没配好」必须能和「网络不通」分开。
 *
 * <p>服务端站点文件损坏时会降级到内置默认站点，那些站点的凭据是空的。若不做这个区分，玩家只会
 * 看到「连接失败，请检查网络」，管理员就会去查防火墙——而真正该做的是补配凭据。本轮 P0 的代价
 * 正来自一句指错方向的提示。</p>
 *
 * <p>内置默认站点必须判为「未配好」，否则这条分流永远不会触发，等于没写。</p>
 */
class STTSiteCredentialsTest {
    @BeforeAll
    static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void aliyunNeedsBothSecretKeyAndAppKey() {
        STTAliyunSite site = new STTAliyunSite.Serializer().defaultSite();
        assertFalse(site.hasUsableCredentials(), "内置默认站点的凭据是空的，必须判为未配好");

        site.setSecretKey("secret");
        assertFalse(site.hasUsableCredentials(), "只填了一半凭据不算配好");

        site.setAppKey("app");
        assertTrue(site.hasUsableCredentials());
    }

    @Test
    void tencentNeedsBothSecretIdAndSecretKey() {
        STTTencentSite site = new STTTencentSite.Serializer().defaultSite();
        assertFalse(site.hasUsableCredentials(), "内置默认站点的凭据是空的，必须判为未配好");

        site.setSecretId("id");
        assertFalse(site.hasUsableCredentials(), "只填了一半凭据不算配好");

        site.setSecretKey("secret");
        assertTrue(site.hasUsableCredentials());
    }

    @Test
    void siliconflowNeedsItsSecretKey() {
        STTSiliconflowSite site = new STTSiliconflowSite.Serializer().defaultSite();
        assertFalse(site.hasUsableCredentials(), "内置默认站点的凭据是空的，必须判为未配好");

        site.setSecretKey("secret");
        assertTrue(site.hasUsableCredentials());
    }

    @Test
    void blankIsNotTreatedAsConfigured() {
        STTAliyunSite site = new STTAliyunSite.Serializer().defaultSite();
        site.setSecretKey("   ");
        site.setAppKey("\t");
        assertFalse(site.hasUsableCredentials(),
                "全空白的凭据同样发不出请求，不能算配好");
    }
}
