package com.github.tartaricacid.touhoulittlemaid.util;


import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;


/**
 * From: https://github.com/mekanism/Mekanism/blob/master/src/main/java/mekanism/common/util/VoxelShapeUtils.java
 * MIT license
 */
public class VoxelShapeUtils {
    private static final Vec3 FROM_ORIGIN = new Vec3(-0.5, -0.5, -0.5);

    /**
     * 打印出一个易于复制粘贴的字符串，表示形状的长方体
     */
    public static void print(double x1, double y1, double z1, double x2, double y2, double z2) {
        TouhouLittleMaid.LOGGER.info("box({}, {}, {}, {}, {}, {}),", Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    /**
     * 打印出一组字符串，使复制粘贴更容易，以简化体素形状
     */
    public static void printSimplified(String name, VoxelShape shape) {
        TouhouLittleMaid.LOGGER.info("Simplified: {}", name);
        shape.optimize().toAabbs().forEach(box -> print(box.minX * 16, box.minY * 16, box.minZ * 16, box.maxX * 16, box.maxY * 16, box.maxZ * 16));
    }

    /**
     * 将 {@link AABB} 旋转到特定一侧，类似于块状态旋转模型的方式。
     *
     * @param box 要旋转的 {@link AABB}
     * @param side 将其旋转到的一侧。
     * @return 旋转后的 {@link AABB}
     */
    public static AABB rotate(AABB box, Direction side) {
        return switch (side) {
            case DOWN -> box;
            case UP -> new AABB(box.minX, -box.minY, -box.minZ, box.maxX, -box.maxY, -box.maxZ);
            case NORTH -> new AABB(box.minX, -box.minZ, box.minY, box.maxX, -box.maxZ, box.maxY);
            case SOUTH -> new AABB(-box.minX, -box.minZ, -box.minY, -box.maxX, -box.maxZ, -box.maxY);
            case WEST -> new AABB(box.minY, -box.minZ, -box.minX, box.maxY, -box.maxZ, -box.maxX);
            case EAST -> new AABB(-box.minY, -box.minZ, box.minX, -box.maxY, -box.maxZ, box.maxX);
        };
    }

    /**
     * 根据特定旋转来旋转 {@link AABB}。
     *
     * @param box 要旋转的 {@link AABB}
     * @param rotation 我们正在执行的轮换。
     * @return 旋转后的 {@link AABB}
     */
    public static AABB rotate(AABB box, Rotation rotation) {
        return switch (rotation) {
            case NONE -> box;
            case CLOCKWISE_90 -> new AABB(-box.minZ, box.minY, box.minX, -box.maxZ, box.maxY, box.maxX);
            case CLOCKWISE_180 -> new AABB(-box.minX, box.minY, -box.minZ, -box.maxX, box.maxY, -box.maxZ);
            case COUNTERCLOCKWISE_90 -> new AABB(box.minZ, box.minY, -box.minX, box.maxZ, box.maxY, -box.maxX);
        };
    }

    /**
     * 将 {@link AABB} 水平旋转到特定侧。这是关于 {@link #rotate(AABB, Rotation)} 的默认最常见旋转设置
     *
     * @param box 要旋转的 {@link AABB}
     * @param side 将其旋转到的一侧。
     * @return 旋转后的 {@link AABB}
     */
    public static AABB rotateHorizontal(AABB box, Direction side) {
        return switch (side) {
            case NORTH -> rotate(box, Rotation.NONE);
            case SOUTH -> rotate(box, Rotation.CLOCKWISE_180);
            case WEST -> rotate(box, Rotation.COUNTERCLOCKWISE_90);
            case EAST -> rotate(box, Rotation.CLOCKWISE_90);
            default -> box;
        };
    }

    /**
     * 将 {@link VoxelShape} 旋转到特定一侧，类似于块状态旋转模型的方式。
     *
     * @param shape 要旋转的 {@link VoxelShape}
     * @param side 将其旋转到的一侧。
     * @return 旋转后的 {@link VoxelShape}
     */
    public static VoxelShape rotate(VoxelShape shape, Direction side) {
        return rotate(shape, box -> rotate(box, side));
    }

    /**
     * 根据特定旋转旋转 {@link VoxelShape}。
     *
     * @param shape 要旋转的 {@link VoxelShape}
     * @param rotation 我们正在执行的轮换。
     * @return 旋转后的 {@link VoxelShape}
     */
    public static VoxelShape rotate(VoxelShape shape, Rotation rotation) {
        return rotate(shape, box -> rotate(box, rotation));
    }

    /**
     * 将 {@link VoxelShape} 水平旋转到特定侧。这是关于 {@link #rotate(VoxelShape, Rotation)} 的默认最常见旋转设置
     *
     * @param shape 要旋转的 {@link VoxelShape}
     * @param side 将其旋转到的一侧。
     * @return 旋转后的 {@link VoxelShape}
     */
    public static VoxelShape rotateHorizontal(VoxelShape shape, Direction side) {
        return rotate(shape, box -> rotateHorizontal(box, side));
    }

    /**
     * 使用 {@link VoxelShape} 中每个 {@link AABB} 的特定变换函数来旋转 {@link VoxelShape}。
     *
     * @param shape 要旋转的 {@link VoxelShape}
     * @param rotateFunction 应用到 {@link VoxelShape} 中每个 {@link AABB} 的变换函数。
     * @return 旋转后的 {@link VoxelShape}
     */
    public static VoxelShape rotate(VoxelShape shape, UnaryOperator<AABB> rotateFunction) {
        List<VoxelShape> rotatedPieces = new ArrayList<>();
        // 将体素形状分解为边界框
        List<AABB> sourceBoundingBoxes = shape.toAabbs();
        // 旋转它们并将它们转换回体素形状
        for (AABB sourceBoundingBox : sourceBoundingBoxes) {
            // 使边界框以中间为中心，旋转后向后移动
            rotatedPieces.add(Shapes.create(rotateFunction.apply(sourceBoundingBox.move(FROM_ORIGIN.x, FROM_ORIGIN.y, FROM_ORIGIN.z))
                    .move(-FROM_ORIGIN.x, -FROM_ORIGIN.z, -FROM_ORIGIN.z)));
        }

        return combine(rotatedPieces);
    }

    /**
     * 用于质量组合形状
     *
     * @param shapes 要包含的 {@link VoxelShape} 列表
     * @return 简化的 {@link VoxelShape} 包括输入形状的所有内容。
     */
    public static VoxelShape combine(VoxelShape... shapes) {
        return batchCombine(Shapes.empty(), BooleanOp.OR, true, shapes);
    }

    /**
     * 用于质量组合形状
     *
     * @param shapes 要包括的 {@link VoxelShape} 的集合
     * @return 简化的 {@link VoxelShape} 包括输入形状的所有内容。
     */
    public static VoxelShape combine(Collection<VoxelShape> shapes) {
        return batchCombine(Shapes.empty(), BooleanOp.OR, true, shapes);
    }

    /**
     * 用于从完整的立方体中切割形状
     *
     * @param shapes 要剪切的 {@link VoxelShape} 列表
     * @return A {@link VoxelShape} 包括不属于输入形状的所有内容。
     */
    public static VoxelShape exclude(VoxelShape... shapes) {
        return batchCombine(Shapes.block(), BooleanOp.ONLY_FIRST, true, shapes);
    }

    /**
     * 用于使用特定的 {@link BooleanOp} 和给定的起始形状进行质量组合形状。
     *
     * @param initial 从 {@link VoxelShape} 开始
     * @param function 要执行的 {@link BooleanOp}
     * @param simplify 如果返回的形状应运行 {@link VoxelShape#optimize()}，则为 True，否则为 False
     * @param shapes 要包括的 {@link VoxelShape} 的集合
     * @return 基于输入参数的 {@link VoxelShape}。
     * @implNote We do not do any simplification until after combining all the shapes, and then only if the {@code simplify} is True. This is because there is a
     * 如果我们仍然有更多的改变，那么每次计算简化形状时的性能都会受到影响。
     */
    public static VoxelShape batchCombine(VoxelShape initial, BooleanOp function, boolean simplify, Collection<VoxelShape> shapes) {
        VoxelShape combinedShape = initial;
        for (VoxelShape shape : shapes) {
            combinedShape = Shapes.joinUnoptimized(combinedShape, shape, function);
        }
        return simplify ? combinedShape.optimize() : combinedShape;
    }

    /**
     * 用于使用特定的 {@link BooleanOp} 和给定的起始形状进行质量组合形状。
     *
     * @param initial 从 {@link VoxelShape} 开始
     * @param function 要执行的 {@link BooleanOp}
     * @param simplify 如果返回的形状应运行 {@link VoxelShape#optimize()}，则为 True，否则为 False
     * @param shapes 要包含的 {@link VoxelShape} 列表
     * @return 基于输入参数的 {@link VoxelShape}。
     * @implNote We do not do any simplification until after combining all the shapes, and then only if the {@code simplify} is True. This is because there is a
     * 如果我们仍然有更多的改变，那么每次计算简化形状时的性能都会受到影响。
     */
    public static VoxelShape batchCombine(VoxelShape initial, BooleanOp function, boolean simplify, VoxelShape... shapes) {
        VoxelShape combinedShape = initial;
        for (VoxelShape shape : shapes) {
            combinedShape = Shapes.joinUnoptimized(combinedShape, shape, function);
        }
        return simplify ? combinedShape.optimize() : combinedShape;
    }
}
