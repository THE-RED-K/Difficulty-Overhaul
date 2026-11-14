package dev.difficultyoverhaul.mixin;

import dev.difficultyoverhaul.DifficultyLevel;
import dev.difficultyoverhaul.config.DifficultyOverhaulConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

/**
 * 创建世界界面（“游戏”标签页）难度按钮替换：
 * 原版难度按钮只有 4 档，这里替换为覆盖全部模式的循环按钮。
 * 选择结果同步到世界难度（非和平一律困难），并记忆到客户端配置，
 * 供集成服务器新建世界时作为初始模式。
 */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
public abstract class CreateWorldScreenGameTabMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void difficultyOverhaul$replaceDifficultyButton(CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof CreateWorldScreen createWorldScreen)) {
            return;
        }
        WorldCreationUiState uiState = createWorldScreen.getUiState();
        GridLayout layout = ((GridLayoutTabAccessor) (Object) this).getLayout();

        // 找到原版难度按钮（值类型为 Difficulty 的 CycleButton）
        CycleButton<?> difficultyButton = null;
        for (LayoutElement element : ((GridLayoutAccessor) (Object) layout).getChildren()) {
            if (element instanceof CycleButton<?> cycleButton && cycleButton.getValue() instanceof Difficulty) {
                difficultyButton = cycleButton;
                break;
            }
        }
        if (difficultyButton == null) {
            return;
        }

        // 记录原按钮所在网格位置，随后从布局中彻底移除。
        // cellInhabitants 与 children 由 addChild 按相同顺序追加，索引一一对应。
        List<LayoutElement> children = ((GridLayoutAccessor) (Object) layout).getChildren();
        List<?> cellInhabitants = ((GridLayoutAccessor) (Object) layout).getCellInhabitants();
        int index = children.indexOf(difficultyButton);
        if (index < 0 || index >= cellInhabitants.size()) {
            return;
        }
        GridLayoutCellInhabitantAccessor inhabitant = (GridLayoutCellInhabitantAccessor) (Object) cellInhabitants.get(index);
        int row = inhabitant.getRow();
        int column = inhabitant.getColumn();
        cellInhabitants.remove(index);
        children.remove(index);

        // 替换为覆盖全部模式的按钮并放回原位
        boolean hardcore = uiState.isHardcore();
        DifficultyLevel initialMode = DifficultyOverhaulConfig.getClientMode();
        CycleButton<DifficultyLevel>[] holder = new CycleButton[1];
        holder[0] = difficultyOverhaul$buildModeButton(uiState, initialMode, hardcore, row, column, layout, holder);

        // 极限模式：屏蔽困难以下（不含困难）模式，仅提供困难及以上的循环值，
        // 因此按钮在极限下依然可用，不再整体禁用。
        // 仅在硬核状态真正变化时重建按钮；按钮回调里的 uiState.setDifficulty
        // 会再次触发本监听器，靠 lastHardcore 比对提前返回，避免递归重建。
        final boolean[] lastHardcore = { hardcore };
        uiState.addListener(state -> {
            if (state.isHardcore() == lastHardcore[0]) {
                return;
            }
            lastHardcore[0] = state.isHardcore();
            DifficultyLevel current = holder[0].getValue();
            holder[0] = difficultyOverhaul$buildModeButton(uiState, current, lastHardcore[0], row, column, layout, holder);
        });
    }

    /** 困难及以上模式（极限模式下供选择的循环值）。 */
    private static final DifficultyLevel[] HARD_AND_ABOVE = Arrays.stream(DifficultyLevel.buttonOrder())
            .filter(level -> level.ordinal() >= DifficultyLevel.HARD.ordinal())
            .toArray(DifficultyLevel[]::new);

    /**
     * 构建/重建难度模式循环按钮：极限模式下仅暴露困难及以上的模式，
     * 若初始值低于困难则回退到困难并同步客户端记忆与世界难度。
     * 重建时会先移除布局中的旧按钮（holder[0] 非空时），再放回原位。
     */
    private static CycleButton<DifficultyLevel> difficultyOverhaul$buildModeButton(
            WorldCreationUiState uiState, DifficultyLevel initialMode, boolean hardcore,
            int row, int column, GridLayout layout, CycleButton<DifficultyLevel>[] holder) {
        DifficultyLevel[] values = hardcore ? HARD_AND_ABOVE : DifficultyLevel.buttonOrder();
        DifficultyLevel init = initialMode;
        if (hardcore && initialMode.ordinal() < DifficultyLevel.HARD.ordinal()) {
            // 极限模式屏蔽困难以下（不含困难）：强制回退到困难并保持一致
            init = DifficultyLevel.HARD;
            DifficultyOverhaulConfig.setClientMode(DifficultyLevel.HARD);
            uiState.setDifficulty(Difficulty.HARD);
        }
        CycleButton<DifficultyLevel> button = CycleButton.builder(DifficultyLevel::getDisplayName)
                .withValues(values)
                .withInitialValue(init)
                .create(0, 0, 210, 20, Component.translatable("options.difficulty"),
                        (btn, mode) -> {
                            btn.setTooltip(Tooltip.create(mode.getInfo()));
                            // 记忆到客户端配置（集成服务器新建世界时作为初始模式）
                            DifficultyOverhaulConfig.setClientMode(mode);
                            // 世界难度：和平→和平，其余一律困难（模组内部也按困难判定）
                            uiState.setDifficulty(mode == DifficultyLevel.PEACEFUL ? Difficulty.PEACEFUL : Difficulty.HARD);
                        });
        button.setTooltip(Tooltip.create(init.getInfo()));

        if (holder[0] != null) {
            List<LayoutElement> children = ((GridLayoutAccessor) (Object) layout).getChildren();
            List<?> cellInhabitants = ((GridLayoutAccessor) (Object) layout).getCellInhabitants();
            int idx = children.indexOf(holder[0]);
            if (idx >= 0) {
                children.remove(idx);
                if (idx < cellInhabitants.size()) {
                    cellInhabitants.remove(idx);
                }
            }
        }
        layout.addChild(button, row, column);
        return button;
    }
}
