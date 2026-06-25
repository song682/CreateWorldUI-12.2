package decok.dfcdvadstf.createworldui.api;

import decok.dfcdvadstf.createworldui.Tags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 *     难度应用器<br>
 *     用于在IntegratedServer.loadAllWorlds完成后应用之前保存的难度锁定设置
 * </p>
 * <p>
 *     Difficulty applier<br>
 *     Used to apply previously saved difficulty lock settings after IntegratedServer.loadAllWorlds completes
 * </p>
 */
public class DifficultyApplier {
    private static final Logger logger = LogManager.getLogger(Tags.NAME + ":DifficultyApplier");

    /** 待应用的难度锁定状态（世界加载时生效） */
    private static boolean pendingDifficultyLocked = false;

    /**
     * 设置下一个创建的世界中难度是否应被锁定
     * @param locked true 表示锁定难度
     */
    public static void setDifficultyLocked(boolean locked) {
        pendingDifficultyLocked = locked;
        logger.info("Pending difficulty lock set to: {}", locked);
    }

    /**
     * 获取并清除待应用的难度锁定状态（消费模式）
     * @return 是否应锁定难度
     */
    public static boolean consumeDifficultyLocked() {
        boolean locked = pendingDifficultyLocked;
        pendingDifficultyLocked = false;
        return locked;
    }

    /** 检查是否有待应用的难度锁定状态 */
    public static boolean hasPendingDifficultyLock() {
        return pendingDifficultyLocked;
    }
}
