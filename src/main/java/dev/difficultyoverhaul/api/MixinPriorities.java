package dev.difficultyoverhaul.api;

/**
 * 本模组 Mixin 的优先级常量（单一事实来源）。
 *
 * <p><b>为什么"接管型"mixin 要用低优先级</b>：
 * <ol>
 *   <li>Mixin 按优先级<b>升序</b>应用 —— {@code MixinInfo#compareTo} 的字节码为
 *       {@code return this.priority - other.priority}（同级再按注册顺序 {@code order}），
 *       故优先级越低越早写入目标方法。</li>
 *   <li>一个方法被某个 mixin {@code @Overwrite} 之后，其它模组的注入
 *       （{@code @Inject} / {@code @ModifyVariable} / MixinExtras 的 {@code @WrapOperation} 等）
 *       能否落入该方法体，取决于 {@code InjectionPoint#checkPriority(mergedPriority, myPriority)}：
 *       其字节码等价于 {@code mergedPriority < myPriority} —— <b>注入方的优先级必须严格高于接管方</b>，
 *       否则在 PREINJECT 阶段抛出
 *       {@code InvalidInjectionException: ... cannot inject into ... merged by ... with priority ...}；
 *       由于 {@code injectors.defaultRequire = 1}，这会直接导致游戏启动崩溃。</li>
 *   <li>反过来，{@code @Overwrite} 之间的冲突是"<b>先写入者胜出</b>"：后写入者被
 *       {@code Method overwrite conflict ... Skipping method} 跳过。因此低优先级同时保证
 *       本模组的接管不会被别的 {@code @Overwrite} 取代 —— 两头都稳。</li>
 * </ol>
 *
 * <p><b>使用范围（2026-11-21 起）</b>：本版唯一接管型 mixin 是
 * {@code DifficultyInstanceMixin#calculateDifficulty}（移除原版两处"拍脑袋"分支），
 * 使用 {@link #TAKE_OVER_FINAL}（{@code -Integer.MAX_VALUE}）——必须<b>最先写入</b>，
 * 任何模组都不能取代或早于它；其它模组的注入仍可叠加其上（注入方优先级只需严格大于该值，恒成立）。
 * 其余 mixin 都是注入 / 重定向类型，一律保持默认优先级 1000 —— 若把它们也压低，
 * 本模组自己的注入会变成"底层"，反而可能被模组后写入的 {@code @Overwrite} 冲掉。
 *
 * <p>Mixin 默认优先级是 1000，生态中绝大多数 mixin 配置都使用默认值（实测某大型整合包 793 个配置：
 * 792 个 1000、1 个 {@code Integer.MAX_VALUE}，无一低于 1000）—— 这是"低于默认值即可让外部注入叠加"
 * 的实测依据。若某个整合包存在优先级 ≤ 本模组接管值的模组仍冲突，按 {@link #TAKE_OVER_OVER}、
 * {@link #TAKE_OVER_OVER_OVER} … 继续下调即可，下面的常量阶梯就是为此准备的。
 *
 * @author THEREDK
 */
public final class MixinPriorities {

    /**
     * 接管型（{@code @Overwrite}）mixin 的优先级：低于 Mixin 默认值 1000，
     * 确保其它模组的注入仍可落入本模组接管后的方法体，同时本模组的接管不被取代。
     */
    public static final int TAKE_OVER = 500;

    public static final int TAKE_OVER_OVER = 250;

    public static final int TAKE_OVER_OVER_OVER = 0;

    public static final int TAKE_OVER_OVER_OVER_OVER = -250;

    public static final int TAKE_OVER_OVER_OVER_OVER_OVER = -500;

    public static final int TAKE_OVER_OVER_OVER_OVER_OVER_OVER = -750;

    public static final int TAKE_OVER_OVER_OVER_OVER_OVER_OVER_OVER = -1000;

    public static final int TAKE_OVER_FINAL = -Integer.MAX_VALUE;

    public static final int WRAPPER = 750;

    /**
     * 最高优先级（最后应用）：供"必须最后定案"的守卫类使用。
     *
     * <p>目前本版本无使用者，保留占位：若未来需要"取消式注入"守卫类
     * （HEAD 处取消，令原版方法体根本不执行），应独立成类并取该值。
     */
    public static final int FINAL = Integer.MAX_VALUE;

    private MixinPriorities() {
    }
}