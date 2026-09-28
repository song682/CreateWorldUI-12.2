package decok.dfcdvadstf.createworldui.mixin.access;

import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * <p>Accessor mixin interface exposing the private fields of vanilla {@code GuiCreateWorld}.</p>
 * <p>Well, vanilla hides everything behind {@code private} — so we use Mixin's {@link Accessor}
 * to auto-generate getters/setters at runtime. External mods (or our own non-mixin code) can
 * just cast the screen instance to {@link IGuiCreateWorldAccess} and read/write state directly —
 * no need to write another mixin or reflect into private fields.</p>
 *
 * <p>暴露原版 {@code GuiCreateWorld} 私有字段的 Accessor mixin 接口。</p>
 * <p>嗯，原版把所有字段都 {@code private} 掉了——所以我们用 Mixin 的 {@link Accessor}
 * 在运行时自动生成 getter/setter。外部模组（或者我们自己的非 mixin 代码）只要把屏幕实例
 * 强转成 {@link IGuiCreateWorldAccess} 就能直接读写状态——不用再写一个 mixin，
 * 也不用反射去戳私有字段。</p>
 *
 * <h3>Why {@code @Mixin} on the interface? / 为什么接口上要加 {@code @Mixin}？</h3>
 * <p>Because Mixin only processes {@link Accessor @Accessor} methods on types it actually
 * recognizes as mixins. A plain interface without {@code @Mixin} — even if it's implemented
 * by another mixin class — won't get accessor bodies generated, and calls would explode
 * with {@code AbstractMethodError} at runtime. The interface also needs to be listed in
 * {@code mixins.createworldui.json} so the config loader feeds it to the Mixin engine.</p>
 * <p>因为 Mixin 只会处理它认得的 mixin 类型上的 {@link Accessor @Accessor} 方法。一个
 * 没加 {@code @Mixin} 的普通接口——即使被另一个 mixin 类 implements 了——也不会被生成
 * accessor 方法体，调用时运行时会直接炸 {@code AbstractMethodError}。
 * 接口还需要被列在 {@code mixins.createworldui.json} 里，配置加载器才会把它送进 Mixin 引擎。</p>
 *
 * <h3>Why under {@code mixin.*} package? / 为什么放在 {@code mixin.*} 包下？</h3>
 * <p>Because Mixin's {@code package} config entry "seals" that package and all its subpackages —
 * classes inside become mixin templates and cannot be referenced directly by game code.
 * So this interface has to live inside the mixin package tree, not under {@code api/}.
 * External code still accesses it just fine via import + cast — the sealing only blocks
 * {@code Class.forName}-style direct loads from outside the mixin pipeline.</p>
 * <p>因为 Mixin 配置里的 {@code package} 字段会"封印"那个包及其所有子包——里面的类被当成
 * mixin 模板，无法被游戏代码直接引用。所以这个接口必须住在 mixin 包树下，不能放在 {@code api/} 里。
 * 外部代码通过 import + 强转访问它没问题——"封印"只挡从 mixin 流水线外走 {@code Class.forName}
 * 之类的直接加载。</p>
 *
 * <h3>Usage / 用法</h3>
 * <pre>{@code
 * GuiCreateWorld gui = ...;
 * IGuiCreateWorldAccess access = (IGuiCreateWorldAccess)(Object) gui;
 * String name = access.createWorldUI$getWorldName();
 * access.createWorldUI$setWorldName("My Shiny New World");
 * }</pre>
 *
 * <h3>Notes / 注意</h3>
 * <ul>
 *   <li>All accessor methods carry the {@code createWorldUI$} prefix to avoid name clashes
 *       with other mods that might define their own accessor interfaces.</li>
 *   <li>所有 accessor 方法都带 {@code createWorldUI$} 前缀，避免跟其他模组自定义的接口撞名。</li>
 * </ul>
 */
@Mixin(GuiCreateWorld.class)
public interface IGuiCreateWorldAccess {

    // === World name / 世界名称 ===
    @Accessor("worldName") String createWorldUI$getWorldName();
    @Accessor("worldName") void   createWorldUI$setWorldName(String value);

    // === Vanilla world name text field / 原版世界名称输入框 ===
    // Vanilla reads this field's text (not the {@code worldName} field) as the world's
    // display name when creating the world, so MOD input must be pushed back into it
    // before creation.
    // 原版创建世界时读取的是这个输入框的文本（而非 {@code worldName} 字段）作为世界的
    // 显示名，因此 MOD 的输入必须在创建前写回这里。
    @Accessor("worldNameField") GuiTextField createWorldUI$getWorldNameField();

    // === Vanilla save-dir name recalculation / 原版存档目录名重算 ===
    // Recomputes {@code saveDirName} from the text field (sanitizes illegal characters,
    // resolves name collisions). Must be invoked after syncing the world name.
    // 从输入框重算 {@code saveDirName}（替换非法字符、处理重名冲突）。同步世界名后必须调用。
    @Invoker("calcSaveDirName") void createWorldUI$invokeCalcSaveDirName();

    // === Game mode / 游戏模式 ===
    @Accessor("gameMode") String createWorldUI$getGameMode();
    @Accessor("gameMode") void   createWorldUI$setGameMode(String value);

    // === Seed / 种子 ===
    @Accessor("worldSeed") String createWorldUI$getSeed();
    @Accessor("worldSeed") void   createWorldUI$setSeed(String value);

    // === Vanilla seed text field / 原版种子输入框 ===
    // Vanilla parses the seed from this field's text (not the {@code worldSeed} field)
    // when creating the world, so MOD input must be pushed back into it before creation.
    // 原版创建世界时解析的是这个输入框的文本（而非 {@code worldSeed} 字段）作为世界
    // 种子，因此 MOD 的输入必须在创建前写回这里。
    @Accessor("worldSeedField") GuiTextField createWorldUI$getWorldSeedField();

    // === World type index / 世界类型索引 ===
    @Accessor("selectedIndex") int  createWorldUI$getWorldTypeIndex();
    @Accessor("selectedIndex") void createWorldUI$setWorldTypeIndex(int value);

    // === Generate structures / 生成建筑 ===
    @Accessor("generateStructuresEnabled") boolean createWorldUI$getGenerateStructures();
    @Accessor("generateStructuresEnabled") void    createWorldUI$setGenerateStructures(boolean value);

    // === Bonus chest / 奖励箱 ===
    @Accessor("bonusChestEnabled") boolean createWorldUI$getBonusChest();
    @Accessor("bonusChestEnabled") void    createWorldUI$setBonusChest(boolean value);

    // === Allow cheats / 允许作弊 ===
    @Accessor("allowCheats") boolean createWorldUI$getAllowCheats();
    @Accessor("allowCheats") void    createWorldUI$setAllowCheats(boolean value);

    // === Hardcore / 硬核模式 ===
    @Accessor("hardCoreMode") boolean createWorldUI$getHardcore();
    @Accessor("hardCoreMode") void    createWorldUI$setHardcore(boolean value);

    // === Parent screen (read-only) / 父界面（只读） ===
    @Accessor("parentScreen") GuiScreen createWorldUI$getParentScreen();
}
