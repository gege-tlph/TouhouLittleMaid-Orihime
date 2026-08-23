package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.implement;

import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.EntityGraphics;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.IChatBubbleRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.texture.GifTexture;
import com.github.tartaricacid.touhoulittlemaid.client.resource.listener.EmojiReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EmojiChatBubbleRenderer implements IChatBubbleRenderer {
    /** 本会话已注册的 GIF 表情贴图（替代基准直接读已私有化的 byPath） */
    private static final Set<Identifier> REGISTERED_GIFS = Collections.synchronizedSet(new HashSet<>());
    private final int width;
    private final int height;
    private final Identifier bg;
    private final Identifier emoji;

    public EmojiChatBubbleRenderer(Identifier bg) {
        this.bg = bg;
        var randomEmojis = EmojiReloadListener.getRandomEmojis();
        if (randomEmojis.isPresent()) {
            var emojiRes = randomEmojis.get();
            this.emoji = emojiRes.location();
            this.width = emojiRes.width();
            this.height = emojiRes.height();
            // 如果是 gif 表情的话，需要手动注册
            // 宿主迁移期以 FIXME 搁置了 gif 动图；缺注册时抽到 GIF 表情 = 空气泡（1.21.11 实机实证过），已还原
            if (emojiRes.isGif()) {
                this.registerGifImage();
            }
        } else {
            // 如果没有表情资源，就使用一个默认的空白资源
            this.emoji = IdentifierUtil.modLoc("textures/chat_bubble/maid_emoji/emoji_0.png");
            this.width = 24;
            this.height = 24;
        }
    }

    private void registerGifImage() {
        // 26.1.2：TextureManager.byPath 已私有化，注册去重由本类自持集合承担；
        // register 只入表不调 loadContents（ReloadableTexture 永不上传）→ 必须 registerAndLoad，
        // 且须渲染线程——非同线程时用 Minecraft.execute 调度（recordRenderCall 已删）
        if (!REGISTERED_GIFS.add(this.emoji)) {
            return;
        }
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        if (!Minecraft.getInstance().isSameThread()) {
            Minecraft.getInstance().execute(() -> manager.registerAndLoad(this.emoji, new GifTexture(this.emoji)));
        } else {
            manager.registerAndLoad(this.emoji, new GifTexture(this.emoji));
        }
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public void render(EntityMaidRenderer renderer, EntityGraphics graphics) {
        graphics.blit(this.emoji, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
    }

    @Override
    public Identifier getBackgroundTexture() {
        return this.bg;
    }
}
