package com.github.tartaricacid.touhoulittlemaid.entity.chatbubble;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidDamageEvent;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.datapack.KaomojiData;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.EmojiChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;

public final class RandomEmoji {
    static void tick(EntityMaid maid) {
        if (!ServerRuleConfig.get(MaidConfig.ENABLE_EMOJI)) {
            return;
        }
        int checkRate = ServerRuleConfig.get(MaidConfig.EMOJI_CHECK_RATE);
        long offset = maid.getUUID().getLeastSignificantBits() % checkRate;
        if ((maid.tickCount + offset) % checkRate != 0) {
            return;
        }
        ChatBubbleManager bubbleManager = maid.getChatBubbleManager();
        boolean empty = bubbleManager.getChatBubbleDataCollection().isEmpty();
        if (!empty) {
            return;
        }
        // 依据权重随机选择表情包类型
        int imageWeight = ServerRuleConfig.get(MaidConfig.IMAGE_EMOJI_WEIGHT);
        int kaomojiWeight = ServerRuleConfig.get(MaidConfig.KAOMOJI_EMOJI_WEIGHT);
        int totalWeight = imageWeight + kaomojiWeight;
        int randomWeight = maid.getRandom().nextInt(totalWeight);
        if (randomWeight < imageWeight) {
            bubbleManager.addChatBubble(EmojiChatBubbleData.create());
        } else {
            KaomojiData.showRoutineKaomoji(maid, bubbleManager);
        }
    }

    public static void addHurtChatText(MaidDamageEvent event) {
        if (!ServerRuleConfig.get(MaidConfig.ENABLE_EMOJI)) {
            return;
        }
        EntityMaid maid = event.getMaid();
        ChatBubbleManager bubbleManager = maid.getChatBubbleManager();
        boolean empty = bubbleManager.getChatBubbleDataCollection().isEmpty();
        if (empty) {
            KaomojiData.showHurtKaomoji(maid, bubbleManager);
        }
    }
}
