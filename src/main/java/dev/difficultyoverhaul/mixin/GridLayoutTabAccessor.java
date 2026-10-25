package dev.difficultyoverhaul.mixin;

import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 访问 GridLayoutTab 的受保护布局字段。 */
@Mixin(GridLayoutTab.class)
public interface GridLayoutTabAccessor {
    @Accessor("layout")
    GridLayout getLayout();
}
