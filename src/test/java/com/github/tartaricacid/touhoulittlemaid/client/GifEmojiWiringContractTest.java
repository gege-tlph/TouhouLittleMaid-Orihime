package com.github.tartaricacid.touhoulittlemaid.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GIF 表情气泡的接线契约。三条各对应一个<b>实证过的静默失效形态</b>：
 * <ol>
 *   <li>宿主以 {@code FIXME} 把 {@code registerGifImage()} 调用注释掉——抽到 GIF 表情时贴图
 *       从未注册 = 空气泡（1.21.11 实机「有的显示有的只剩气泡框」的根因）。注释剥掉后断言调用存在。</li>
 *   <li>26.1.2 的 {@code TextureManager.register} 只入表不调 loadContents，ReloadableTexture
 *       永不上传——注册必须走 {@code registerAndLoad}，裸 register 同样表现为空气泡。</li>
 *   <li>{@code GifTexture} 曾被误判「Tickable 接口不存在」而注释掉 implements——tick 永不被调，
 *       GIF 冻在首帧。断言 implements TickableTexture 仍在。</li>
 * </ol>
 */
class GifEmojiWiringContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final String PKG = "src/main/java/com/github/tartaricacid/touhoulittlemaid";
    private static final String RENDERER = PKG + "/client/renderer/entity/chatbubble/implement/EmojiChatBubbleRenderer.java";
    private static final String TEXTURE = PKG + "/client/renderer/texture/GifTexture.java";

    @Test
    void gifBranchRegistersTheTexture() throws IOException {
        String body = methodBody(RENDERER, "public EmojiChatBubbleRenderer(");
        assertTrue(body.contains("registerGifImage()"),
                "构造器的 isGif 分支没有调用 registerGifImage()——宿主搁置形态复发，GIF 表情将是空气泡");
    }

    @Test
    void registrationLoadsTheReloadableTexture() throws IOException {
        String body = methodBody(RENDERER, "void registerGifImage()");
        assertTrue(body.contains("registerAndLoad"),
                "registerGifImage 没走 registerAndLoad——26.1.2 的 register 只入表不上传，GIF 表情将是空气泡");
    }

    @Test
    void gifTextureIsTickable() throws IOException {
        String source = stripComments(Files.readString(ROOT.resolve(TEXTURE), StandardCharsets.UTF_8));
        assertTrue(source.contains("implements TickableTexture"),
                "GifTexture 不再 implements TickableTexture——tick 永不被调，GIF 冻在首帧（历史上真发生过）");
    }

    private static String methodBody(String relativeFile, String methodMarker) throws IOException {
        String source = stripComments(Files.readString(ROOT.resolve(relativeFile), StandardCharsets.UTF_8));
        int markerAt = source.indexOf(methodMarker);
        assertTrue(markerAt >= 0, relativeFile + " 里找不到方法 " + methodMarker);
        assertEquals(-1, source.indexOf(methodMarker, markerAt + 1),
                relativeFile + " 里 " + methodMarker + " 出现多次，方法体定位不再唯一，本测试需要更精确的标记");
        int open = source.indexOf('{', markerAt);
        assertTrue(open >= 0, relativeFile + " 的 " + methodMarker + " 后找不到方法体");
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        throw new AssertionError(relativeFile + " 的 " + methodMarker + " 方法体花括号不配对");
    }

    /** 剥掉块注释（含 javadoc）与行注释——被搁置的调用正是以注释形态存在的，剥掉后就不会误判为「已接线」。 */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }
}
