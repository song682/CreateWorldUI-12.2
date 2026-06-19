package decok.dfcdvadstf.createworldui.command;

import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.gamerule.GuiScreenGameRuleEditor;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *     Command to open the GameRule Editor GUI in-game.<br>
 *     Usage: /gameruleEditor<br>
 *     Requires igGameruleEdit to be enabled in config.
 * </p>
 * <p>
 *     用于在游戏中打开游戏规则编辑器界面的命令。<br>
 *     用法：/gameruleEditor<br>
 *     需要在配置中启用igGameruleEdit。
 * </p>
 */
public class CommandGameRuleEditor extends CommandBase {

    @Override
    @MethodsReturnNonnullByDefault
    public String getName() {
        return "gameruleEditor";
    }

    @Override
    @MethodsReturnNonnullByDefault
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/gameruleEditor";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // Same as /gamerule command
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) {
        // Check if igGameruleEdit is enabled in config, or if ModernDifficultyLocker is loaded
        // 检查配置中是否启用了igGameruleEdit，或是否加载了ModernDifficultyLocker
        boolean modernDifficultyLockerLoaded = Loader.isModLoaded("difficultylocker");
        if (CreateWorldUI.config == null || (!CreateWorldUI.config.igGameruleEdit && !modernDifficultyLockerLoaded)) {
            return;
        }

        // Check if this is a client-side execution (has Minecraft client)
        // 检查是否是客户端执行（有Minecraft客户端实例）
        if (Minecraft.getMinecraft() == null) {
            return;
        }

        // Get current world's game rules
        // 获取当前世界的游戏规则
        World world = Minecraft.getMinecraft().world;
        Map<String, String> gameRules = new HashMap<>();

        if (world != null) {
            GameRules rules = world.getGameRules();
            for (String ruleName : rules.getRules()) {
                gameRules.put(ruleName, rules.getString(ruleName));
            }
        }

        // Open the GameRule Editor GUI
        // 打开游戏规则编辑器界面
        // Use null as parent screen since we're coming from in-game command
        // 使用null作为父界面，因为我们是从游戏内命令调用的
        Minecraft.getMinecraft().displayGuiScreen(
                new GuiScreenGameRuleEditor(null, gameRules)
        );
    }
}
