package dev.difficultyoverhaul.mixin;

import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** 访问 GridLayout 的私有子项列表，以便移除并替换难度按钮。 */
@Mixin(GridLayout.class)
public interface GridLayoutAccessor {
    @Accessor("children")
    List<LayoutElement> getChildren();

    @Accessor("cellInhabitants")
    List<?> getCellInhabitants();
}
