package dev.difficultyoverhaul.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问 GridLayout 内部单元格记录的字段，
 * 用于定位难度按钮所在网格位置（children 与 cellInhabitants 顺序一致）。
 */
@Mixin(targets = "net.minecraft.client.gui.layouts.GridLayout$CellInhabitant")
public interface GridLayoutCellInhabitantAccessor {
    @Accessor("row")
    int getRow();

    @Accessor("column")
    int getColumn();
}
