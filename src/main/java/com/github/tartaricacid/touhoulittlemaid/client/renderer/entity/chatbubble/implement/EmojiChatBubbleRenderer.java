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

public class EmojiChatBubbleRenderer implements IChatBubbleRenderer {
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
            // SWEEP R13-3：原 FIXME 删除了 GIF 注册 → 抽到 GIF 表情时贴图从未注册 = 空气泡
            // （入世实测「有的显示有的只剩气泡框」的根因；GifTexture 本树已编译）。还原 origin；
            // 1.21.11 RenderSystem.recordRenderCall 已删 → 用 Minecraft.execute 调度到客户端主线程（=渲染线程）
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
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        if (manager.byPath.containsKey(this.emoji)) {
            return;
        }
        if (!Minecraft.getInstance().isSameThread()) {
            Minecraft.getInstance().execute(() -> manager.register(this.emoji, new GifTexture(this.emoji)));
        } else {
            manager.register(this.emoji, new GifTexture(this.emoji));
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
