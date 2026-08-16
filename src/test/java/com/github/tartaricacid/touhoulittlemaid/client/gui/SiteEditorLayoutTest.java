package com.github.tartaricacid.touhoulittlemaid.client.gui;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor.SiteEditorLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 站点编辑屏的横向布局算术。
 *
 * <p>屏幕布局做不到端到端自动验收（构造 {@code Screen} 要真实客户端），
 * 但<b>算术这一层能钉死，就不该靠眼睛看</b>——这几个数原先由 LLM / TTS / STT 三个编辑屏
 * 各写一份字面值，而它们本该完全相同，结果就是各自算错：往只有「添加 / 保存 / 返回」的那一排
 * 里塞「检查配置」时取了 {@code BASE_WIDTH - 302}（= 98），而「添加」占到 108，
 * <b>两颗按钮重叠 10 像素</b>。</p>
 *
 * <p>{@link SiteEditorLayout} 是纯 int 常量、不碰任何 Minecraft 类型，所以这些断言
 * 能在无客户端的 JUnit 环境里跑。</p>
 */
class SiteEditorLayoutTest {
    /** 底部那一排的四颗按钮：名字 + 左端 + 宽度 */
    private record Box(String name, int x, int width) {
        int right() {
            return this.x + this.width;
        }
    }

    private static List<Box> bottomRow() {
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box("添加模型", SiteEditorLayout.ADD_MODEL_X, SiteEditorLayout.ADD_MODEL_WIDTH));
        boxes.add(new Box("检查配置", SiteEditorLayout.CHECK_CONFIG_X, SiteEditorLayout.CHECK_CONFIG_WIDTH));
        boxes.add(new Box("保存", SiteEditorLayout.SAVE_X, SiteEditorLayout.SAVE_WIDTH));
        boxes.add(new Box("返回", SiteEditorLayout.BACK_X, SiteEditorLayout.BACK_WIDTH));
        return boxes;
    }

    @Test
    void bottomRowButtonsNeverOverlap() {
        List<Box> boxes = bottomRow();
        // 活性判据与被断言的量正交：这里数的是「看守了几颗按钮」，不是「有几处重叠」。
        // 用重叠数当活性证明，在「不许重叠」这类禁止型断言上恰恰是反的——零重叠与零覆盖同形。
        assertTrue(boxes.size() >= 4, "底部按钮少于 4 颗，这条断言已失去看守对象");

        List<String> collisions = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) {
            for (int j = i + 1; j < boxes.size(); j++) {
                Box a = boxes.get(i);
                Box b = boxes.get(j);
                if (a.x() < b.right() && b.x() < a.right()) {
                    collisions.add("%s[%d,%d) 与 %s[%d,%d) 重叠"
                            .formatted(a.name(), a.x(), a.right(), b.name(), b.x(), b.right()));
                }
            }
        }
        assertTrue(collisions.isEmpty(), "底部按钮互相重叠：" + collisions);
    }

    @Test
    void bottomRowButtonsKeepTheMinimumGap() {
        List<Box> boxes = bottomRow().stream().sorted((a, b) -> Integer.compare(a.x(), b.x())).toList();
        List<String> tooClose = new ArrayList<>();
        for (int i = 1; i < boxes.size(); i++) {
            int gap = boxes.get(i).x() - boxes.get(i - 1).right();
            if (gap < SiteEditorLayout.GAP) {
                tooClose.add("%s 与 %s 间隙 %d < %d"
                        .formatted(boxes.get(i - 1).name(), boxes.get(i).name(), gap, SiteEditorLayout.GAP));
            }
        }
        assertTrue(tooClose.isEmpty(), "底部按钮挨得太近：" + tooClose);
    }

    @Test
    void bottomRowStaysInsideThePanel() {
        for (Box box : bottomRow()) {
            assertTrue(box.x() >= SiteEditorLayout.MARGIN,
                    box.name() + " 越过了左留白：x=" + box.x());
            assertTrue(box.right() <= SiteEditorLayout.PANEL_WIDTH - SiteEditorLayout.MARGIN,
                    box.name() + " 越过了右留白：right=" + box.right());
        }
    }

    /**
     * 密钥输入框必须给「清除」让位。
     *
     * <p>不让位不只是难看：输入框比按钮先 {@code addWidget}，于是重叠带里的点击由输入框
     * 拿走，按钮下半截按不动——这是一个「看得见但按不动」的失灵，比错位更难查。</p>
     */
    @Test
    void secretInputYieldsRoomForTheClearButton() {
        int secretLeft = SiteEditorLayout.MARGIN;
        int secretRight = secretLeft + SiteEditorLayout.SECRET_WIDTH_WITH_CLEAR;
        assertTrue(secretRight <= SiteEditorLayout.SECRET_CLEAR_X - SiteEditorLayout.GAP,
                "密钥框右缘 %d 没有给「清除」(x=%d) 让出 %d 的间隙"
                        .formatted(secretRight, SiteEditorLayout.SECRET_CLEAR_X, SiteEditorLayout.GAP));
        assertTrue(SiteEditorLayout.SECRET_CLEAR_X + SiteEditorLayout.SECRET_CLEAR_WIDTH
                        <= SiteEditorLayout.PANEL_WIDTH - SiteEditorLayout.MARGIN,
                "「清除」越过了右留白");
        assertTrue(SiteEditorLayout.SECRET_WIDTH_WITH_CLEAR > 0, "让位后密钥框宽度不能为零");
    }
}
