package com.github.tartaricacid.touhoulittlemaid.api;

/**
 * 此注释可以让东方小女仆 mod 检测 mod 扩展名。所有 {@link com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid} 必须具有此注释和不带参数的构造函数。
 */

/**
 * 注意：这用于 (Neo)Forge。 Fabric 使用入口点来替换它，因此您需要在类上实现 {@link com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid} 接口，并将其添加为 Fabric.mod.json 中“little_maid_extension”类型的入口点
 */
@Deprecated
public @interface LittleMaidExtension {
}
