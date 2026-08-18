package decok.dfcdvadstf.createworldui.mixin.middle;

import decok.dfcdvadstf.createworldui.CreateWorldUI;
import decok.dfcdvadstf.createworldui.ui.GuiWorldSettings;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * <p>
 *     Modifies the vanilla Options screen (GuiOptions) via Mixin: when the World Settings
 *     feature is enabled, hides the vanilla difficulty selector and difficulty lock button,
 *     replacing them with a single "World Settings..." button that opens {@link GuiWorldSettings}.
 * </p>
 * <p>
 *     通过 Mixin 修改原版选项界面：当「世界设置」功能启用时，隐藏原版的难度选择与
 *     难度锁定按钮，用统一的 "World Settings..." 按钮替代（点击进入 {@link GuiWorldSettings}）。
 * </p>
 */
@Mixin(GuiOptions.class)
public abstract class MixinGuiOptions extends GuiScreen {

    /** 原版难度按钮 ID（GuiOptions 硬编码，1.12.2 的 GameSettings.Options 已无 DIFFICULTY 项） */
    /** Vanilla difficulty button ID (hardcoded in GuiOptions; GameSettings.Options has no DIFFICULTY in 1.12.2) */
    private static final int VANILLA_DIFFICULTY_BUTTON_ID = 108;
    /** 原版锁定按钮 ID（GuiOptions 硬编码） */
    /** Vanilla lock button ID (hardcoded in GuiOptions) */
    private static final int VANILLA_LOCK_BUTTON_ID = 109;
    /** World Settings 按钮 ID */
    /** World Settings button ID */
    @Unique
    private static final int WORLD_SETTINGS_BUTTON_ID = 5005;

    @Unique
    private GuiButton createWorldUI$worldSettingsButton;
    @Unique
    private GuiButton createWorldUI$vanillaDifficultyButton;
    @Unique
    private GuiButton createWorldUI$vanillaLockButton;

    /**
     * 在 initGui 末尾隐藏原版难度/锁定按钮，并在原难度按钮位置添加 "World Settings..." 按钮。
     * 仅在单人世界（集成服务器）且配置启用时生效，其他场景保持原版布局。
     *
     * At the end of initGui, hide the vanilla difficulty/lock buttons and add the
     * "World Settings..." button at the vanilla difficulty button position.
     * Only active in singleplayer (integrated server) with the config enabled;
     * other scenarios keep the vanilla layout.
     */
    @Inject(method = "initGui", at = @At("TAIL"))
    private void createWorldUI$onInitGuiTail(CallbackInfo ci) {
        // 仅在单人世界（集成服务器）中启用该功能
        // Only enable the feature in singleplayer (integrated server)
        if (mc.world == null || !mc.isIntegratedServerRunning()) return;
        if (CreateWorldUI.config == null || !CreateWorldUI.config.enableWorldSettings) return;

        // 查找原版难度按钮与锁定按钮
        // Find the vanilla difficulty and lock buttons
        for (GuiButton b : (List<GuiButton>) this.buttonList) {
            if (b.id == VANILLA_DIFFICULTY_BUTTON_ID) {
                this.createWorldUI$vanillaDifficultyButton = b;
            } else if (b.id == VANILLA_LOCK_BUTTON_ID) {
                this.createWorldUI$vanillaLockButton = b;
            }
        }
        if (createWorldUI$vanillaDifficultyButton == null) return;

        // 隐藏原版按钮，避免与原版布局重叠
        // Hide the vanilla buttons to avoid overlapping with the vanilla layout
        createWorldUI$vanillaDifficultyButton.visible = false;
        createWorldUI$vanillaDifficultyButton.enabled = false;
        if (createWorldUI$vanillaLockButton != null) {
            createWorldUI$vanillaLockButton.visible = false;
            createWorldUI$vanillaLockButton.enabled = false;
        }

        // 在原难度按钮位置添加 "World Settings..." 按钮
        // Add the "World Settings..." button at the vanilla difficulty button position
        createWorldUI$worldSettingsButton = new GuiButton(
            WORLD_SETTINGS_BUTTON_ID,
            createWorldUI$vanillaDifficultyButton.x,
            createWorldUI$vanillaDifficultyButton.y,
            150, 20,
            I18n.format("createworldui.button.worldSettings")
        );
        this.buttonList.add(createWorldUI$worldSettingsButton);
    }

    /**
     * 处理 "World Settings..." 按钮点击，打开统一的世界设置界面。
     * Handle "World Settings..." button clicks, opening the unified world settings screen.
     */
    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void createWorldUI$onActionPerformedHead(GuiButton button, CallbackInfo ci) {
        if (button.id == WORLD_SETTINGS_BUTTON_ID && button.enabled) {
            mc.displayGuiScreen(new GuiWorldSettings((GuiScreen) (Object) this, mc.gameSettings));
        }
    }

    /**
     * 每帧维护按钮可见性：非单人世界或功能关闭时隐藏 World Settings 按钮；
     * 功能启用时持续保持原版难度/锁定按钮隐藏。
     *
     * Maintain button visibility each frame: hide the World Settings button outside
     * singleplayer or when the feature is disabled; keep the vanilla difficulty/lock
     * buttons hidden while the feature is enabled.
     */
    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void createWorldUI$onDrawScreenTail(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (createWorldUI$worldSettingsButton == null) return;

        boolean show = mc.world != null
            && mc.isIntegratedServerRunning()
            && CreateWorldUI.config != null
            && CreateWorldUI.config.enableWorldSettings;
        createWorldUI$worldSettingsButton.visible = show;

        if (createWorldUI$vanillaDifficultyButton != null) {
            createWorldUI$vanillaDifficultyButton.visible = !show;
            createWorldUI$vanillaDifficultyButton.enabled = !show;
        }
        if (createWorldUI$vanillaLockButton != null) {
            createWorldUI$vanillaLockButton.visible = !show;
            createWorldUI$vanillaLockButton.enabled = !show;
        }
    }
}
