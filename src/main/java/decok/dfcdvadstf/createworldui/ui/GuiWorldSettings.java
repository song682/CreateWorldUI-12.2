package decok.dfcdvadstf.createworldui.ui;

import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleApplier;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter;
import decok.dfcdvadstf.createworldui.api.gamerule.GameRuleMonitorNSetter.GameruleValue;
import decok.dfcdvadstf.createworldui.ui.gamerule.GuiScreenGameRuleEditor;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLockIconButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.init.SoundEvents;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 世界设置界面 - 当配置启用时，替代 GuiOptions 中的难度按钮和锁定按钮，提供统一的世界设置入口。
 * 包含三个主要功能：
 * - 难度选择：循环切换难度（锁定后禁用）
 * - 难度锁定：GuiLockIconButton 六态图标反馈（锁定/未锁定 × 普通/悬停/禁用），Hardcore 自动锁定且不可解锁
 * - 游戏规则编辑：仅在创造模式 + 作弊模式同时启用时可用
 *
 * World settings screen - replaces the vanilla difficulty and lock buttons in GuiOptions
 * when the feature is enabled, providing a unified world settings entry.
 * Contains three main functions:
 * - Difficulty selection: cycles through difficulties (disabled when locked)
 * - Difficulty lock: GuiLockIconButton six-state icon feedback, auto-locked & un-unlockable in Hardcore
 * - Game rule editing: only available when both Creative mode and cheats are enabled
 */
@SuppressWarnings("unchecked")
public class GuiWorldSettings extends GuiScreen implements GuiYesNoCallback {

    private static final Logger LOGGER = LogManager.getLogger("GuiWorldSettings");

    private static final int DIFFICULTY_BUTTON_ID = 5000;
    private static final int LOCK_BUTTON_ID = 5001;
    private static final int GAMERULES_BUTTON_ID = 5003;
    private static final int DONE_BUTTON_ID = 5004;

    private static final int CONFIRM_LOCK_ID = 1001;
    private static final int CONFIRM_UNLOCK_ID = 1002;

    private final GuiScreen parentScreen;
    private final GameSettings gameSettings;

    private GuiButton difficultyButton;
    private GuiLockIconButton lockButton;
    private GuiButton gameRulesButton;

    public GuiWorldSettings(GuiScreen parentScreen, GameSettings gameSettings) {
        this.parentScreen = parentScreen;
        this.gameSettings = gameSettings;
    }

    @Override
    public void initGui() {
        // 如果从 GameRuleEditor 返回，将待生效的游戏规则应用到服务端世界
        // Apply pending game rules to server worlds when returning from the GameRuleEditor
        applyPendingGameRules();

        this.buttonList.clear();

        if (mc.world == null || mc.world.getWorldInfo() == null) {
            mc.displayGuiScreen(parentScreen);
            return;
        }

        WorldInfo worldInfo = mc.world.getWorldInfo();
        boolean isLocked = worldInfo.isDifficultyLocked();
        boolean isHardcore = worldInfo.isHardcoreModeEnabled();

        // Hardcore 模式强制锁定难度（原版难度本就强制为困难，这里同步锁定状态）
        // Hardcore mode forces the difficulty to be locked (vanilla already forces HARD)
        if (isHardcore && !isLocked) {
            worldInfo.setDifficultyLocked(true);
            isLocked = true;
        }

        // === 难度按钮（左侧，缩小宽度给锁定按钮腾空间） ===
        // === Difficulty button (left side, narrowed to make room for the lock button) ===
        int diffBtnX = this.width / 2 - 155;
        int diffBtnY = this.height / 6 - 12;

        difficultyButton = new GuiButton(DIFFICULTY_BUTTON_ID, diffBtnX, diffBtnY, 128, 20, getDifficultyText());
        difficultyButton.enabled = !isLocked;

        // === 锁定按钮（难度按钮右侧） ===
        // === Lock button (right of the difficulty button) ===
        int lockBtnX = diffBtnX + 128 + 2; // 2px间距 / 2px gap
        lockButton = new GuiLockIconButton(LOCK_BUTTON_ID, lockBtnX, diffBtnY);
        lockButton.setLocked(isLocked);
        lockButton.enabled = !isHardcore && (!isLocked || allowUnlock());

        // === 游戏规则按钮（右侧） ===
        // === Game rules button (right side) ===
        gameRulesButton = new GuiButton(GAMERULES_BUTTON_ID,
            this.width / 2 + 5, diffBtnY,
            150, 20, I18n.format("createworldui.button.gameRuleEditor"));

        // 仅在创造模式 + 作弊模式同时启用时可用
        // Only available when both Creative mode and cheats are enabled
        boolean isCreative = isCreativeMode();
        boolean cheatsEnabled = areCheatsEnabled();
        gameRulesButton.enabled = isCreative && cheatsEnabled;

        // === 完成按钮 ===
        // === Done button ===
        GuiButton doneButton = new GuiButton(DONE_BUTTON_ID,
            this.width / 2 - 100, this.height / 6 + 168,
            I18n.format("gui.done"));

        this.buttonList.add(difficultyButton);
        this.buttonList.add(lockButton);
        this.buttonList.add(gameRulesButton);
        this.buttonList.add(doneButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;

        // 难度按钮 - 循环切换难度（与原版 GuiOptions 行为一致）
        // Difficulty button - cycle difficulty (same behavior as vanilla GuiOptions)
        if (button.id == DIFFICULTY_BUTTON_ID) {
            if (mc.world == null || mc.world.getWorldInfo() == null) return;
            EnumDifficulty current = mc.world.getWorldInfo().getDifficulty();
            mc.world.getWorldInfo().setDifficulty(
                EnumDifficulty.getDifficultyEnum(current.getDifficultyId() + 1));
            button.displayString = getDifficultyText();
        }
        // 锁定按钮 - 弹出确认对话框
        // Lock button - open the confirmation dialog
        else if (button.id == LOCK_BUTTON_ID) {
            if (mc.world == null || mc.world.getWorldInfo() == null) return;
            boolean currentLocked = mc.world.getWorldInfo().isDifficultyLocked();

            if (!currentLocked) {
                // 锁定操作（需要确认）——使用原版 difficulty.lock.* 翻译键，与原版 GuiOptions 的锁定确认完全一致
                // Lock action (requires confirmation) - uses the vanilla difficulty.lock.*
                // translation keys, identical to the vanilla GuiOptions lock confirmation
                mc.displayGuiScreen(new GuiYesNo(this,
                    I18n.format("difficulty.lock.title"),
                    I18n.format("difficulty.lock.question",
                        I18n.format(mc.world.getWorldInfo().getDifficulty().getDifficultyResourceKey())),
                    CONFIRM_LOCK_ID));
            } else if (allowUnlock()) {
                // 解锁操作（需要确认）/ Unlock action (requires confirmation)
                mc.displayGuiScreen(new GuiYesNo(this,
                    I18n.format("createworldui.unlock.confirm.title"),
                    I18n.format("createworldui.unlock.confirm.line"),
                    CONFIRM_UNLOCK_ID));
            }
        }
        // 游戏规则按钮 - 打开 GameRuleEditor
        // Game rules button - open the GameRuleEditor
        else if (button.id == GAMERULES_BUTTON_ID) {
            openGameRuleEditor();
        }
        // 完成按钮
        // Done button
        else if (button.id == DONE_BUTTON_ID) {
            gameSettings.saveOptions();
            mc.displayGuiScreen(parentScreen);
        }
    }

    /**
     * GuiYesNoCallback - 确认锁定/解锁对话框的回调
     * GuiYesNoCallback - callback for the lock/unlock confirmation dialogs
     */
    @Override
    public void confirmClicked(boolean confirmed, int id) {
        if (mc.world == null || mc.world.getWorldInfo() == null) {
            mc.displayGuiScreen(this);
            return;
        }

        WorldInfo worldInfo = mc.world.getWorldInfo();
        boolean isHardcore = worldInfo.isHardcoreModeEnabled();

        // 锁定确认 (id=1001) / Lock confirmation (id=1001)
        if (id == CONFIRM_LOCK_ID) {
            if (confirmed) {
                worldInfo.setDifficultyLocked(true);

                if (difficultyButton != null) difficultyButton.enabled = false;
                if (lockButton != null) {
                    lockButton.setLocked(true);
                    if (!allowUnlock()) lockButton.enabled = false;
                }
                playClickSound();
            }
            mc.displayGuiScreen(this);
        }
        // 解锁确认 (id=1002) / Unlock confirmation (id=1002)
        else if (id == CONFIRM_UNLOCK_ID) {
            // Hardcore 模式下不允许解锁
            // Unlocking is forbidden in Hardcore mode
            if (isHardcore) {
                mc.displayGuiScreen(this);
                return;
            }

            if (confirmed) {
                worldInfo.setDifficultyLocked(false);

                if (difficultyButton != null) difficultyButton.enabled = true;
                if (lockButton != null) {
                    lockButton.setLocked(false);
                    lockButton.enabled = true;
                }
                playClickSound();
            }
            mc.displayGuiScreen(this);
        }
    }

    /**
     * 打开 CreateWorldUI 的游戏规则编辑器
     * 传入当前世界的游戏规则作为可编辑数据
     *
     * Open the CreateWorldUI game rule editor
     * Pass the current world's game rules as the editable data
     */
    private void openGameRuleEditor() {
        Map<String, String> currentRules = new HashMap<>();
        // 必须从服务端世界读取规则：客户端 WorldClient 的 GameRules 只是原版默认值副本（并非同步副本），
        // 模组新增的规则只存在于服务端世界。（统一通过 getRuleSourceWorld() 选择规则来源）
        // Must read rules from the SERVER-side world: the client WorldClient GameRules is
        // only a vanilla-default copy (not a synced copy); mod-added rules exist solely in
        // the server world. (Rule source unified via getRuleSourceWorld())
        World ruleSource = GameRuleMonitorNSetter.getRuleSourceWorld();
        if (ruleSource != null) {
            Map<String, GameruleValue> allRules = GameRuleMonitorNSetter.getAllGamerules(ruleSource);
            for (Map.Entry<String, GameruleValue> entry : allRules.entrySet()) {
                currentRules.put(entry.getKey(), entry.getValue().stringValue);
            }
        }
        mc.displayGuiScreen(new GuiScreenGameRuleEditor(this, currentRules));
    }

    /**
     * 将 GameRuleEditor 保存的待应用规则直接应用到当前运行的世界（服务端所有维度）。
     * GameRuleEditor 默认使用 GameRuleApplier.setPendingGameRules（设计为世界加载时生效），
     * 已在运行的世界需要直接应用。
     *
     * Apply the pending rules saved by the GameRuleEditor directly to the running world
     * (all server dimensions). The editor defaults to GameRuleApplier.setPendingGameRules
     * (designed to take effect on world load); a running world needs direct application.
     */
    private void applyPendingGameRules() {
        try {
            Map<String, String> pending = GameRuleApplier.getPendingGameRules();
            if (pending == null || pending.isEmpty() || mc.world == null) return;

            // 应用到服务端所有维度（客户端世界已由编辑器自身应用）
            // Apply to all server dimensions (the client world is applied by the editor itself)
            if (mc.getIntegratedServer() != null
                && mc.getIntegratedServer().worlds != null) {
                for (WorldServer ws : mc.getIntegratedServer().worlds) {
                    if (ws != null) {
                        for (Map.Entry<String, String> entry : pending.entrySet()) {
                            ws.getGameRules().setOrCreateGameRule(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }

            LOGGER.info("Applied {} game rules to current world from GameRuleEditor", pending.size());
            pending.clear();
        } catch (Exception e) {
            LOGGER.error("Failed to apply pending game rules: {}", e.getMessage());
        }
    }

    /**
     * 获取当前难度按钮的显示文本（Hardcore 显示固定文本）
     * Get the display text of the difficulty button (Hardcore shows a fixed text)
     */
    private String getDifficultyText() {
        if (mc.world == null || mc.world.getWorldInfo() == null) return "";
        WorldInfo worldInfo = mc.world.getWorldInfo();
        if (worldInfo.isHardcoreModeEnabled()) {
            return I18n.format("options.difficulty") + ": " + I18n.format("options.difficulty.hardcore");
        }
        return I18n.format("options.difficulty") + ": " + I18n.format(worldInfo.getDifficulty().getDifficultyResourceKey());
    }

    private boolean allowUnlock() {
        return CreateWorldUI.config != null && CreateWorldUI.config.allowUnlock;
    }

    private boolean isCreativeMode() {
        try {
            if (mc.world != null && mc.world.getWorldInfo() != null) {
                return mc.world.getWorldInfo().getGameType().isCreative();
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean areCheatsEnabled() {
        try {
            if (mc.getIntegratedServer() != null
                && mc.getIntegratedServer().worlds != null) {
                for (WorldServer ws : mc.getIntegratedServer().worlds) {
                    if (ws != null && ws.getWorldInfo() != null) {
                        return ws.getWorldInfo().areCommandsAllowed();
                    }
                }
            }
            if (mc.world != null && mc.world.getWorldInfo() != null) {
                return mc.world.getWorldInfo().areCommandsAllowed();
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void playClickSound() {
        if (mc != null && mc.getSoundHandler() != null) {
            mc.getSoundHandler().playSound(
                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // ESC 键返回时保存设置（与原版 GuiOptions 行为一致）
        // Save settings when returning with ESC (same behavior as vanilla GuiOptions)
        if (keyCode == 1) {
            gameSettings.saveOptions();
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer,
            I18n.format("createworldui.worldsettings.title"),
            this.width / 2, 15, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 为禁用的游戏规则按钮绘制 Tooltip
        // Draw a tooltip for the disabled game rules button
        if (gameRulesButton != null && !gameRulesButton.enabled) {
            boolean hovered = mouseX >= gameRulesButton.x &&
                mouseX < gameRulesButton.x + gameRulesButton.width &&
                mouseY >= gameRulesButton.y &&
                mouseY < gameRulesButton.y + gameRulesButton.height;

            if (hovered) {
                List<String> tooltip = new ArrayList<>();
                boolean isCreative = isCreativeMode();
                boolean cheatsEnabled = areCheatsEnabled();

                if (!isCreative && !cheatsEnabled) {
                    tooltip.add(I18n.format("createworldui.gamerules.tooltip.needCreativeAndCheats"));
                } else if (!isCreative) {
                    tooltip.add(I18n.format("createworldui.gamerules.tooltip.needCreative"));
                } else {
                    tooltip.add(I18n.format("createworldui.gamerules.tooltip.needCheats"));
                }

                this.drawHoveringText(tooltip, mouseX, mouseY);
            }
        }
    }
}
