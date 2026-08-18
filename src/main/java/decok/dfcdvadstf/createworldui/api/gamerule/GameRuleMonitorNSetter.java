package decok.dfcdvadstf.createworldui.api.gamerule;

import decok.dfcdvadstf.createworldui.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>
 *     游戏规则监控与设置器<br>
 *     提供游戏规则的获取、设置、添加等操作，支持多种数据类型
 * </p>
 * <p>
 *     Game rule monitor and setter<br>
 *     Provides operations such as getting, setting, adding game rules, supporting multiple data types
 * </p>
 */
@SideOnly(Side.CLIENT)
public class GameRuleMonitorNSetter {

    private static final Logger LOGGER = LogManager.getLogger(Tags.NAME + ":GameruleMonitorAndSetter");

    /**
     * 游戏规则值容器类，包含所有可能的数据类型<br>
     * Game rule value container class with all possible data types
     */

    public static class GameruleValue {


        public final String stringValue; // 字符串形式的值 / Value in string form
        public final boolean booleanValue; // 布尔形式的值 / Value in boolean form
        public final int intValue; // 整数形式的值 / Value in integer form
        public final double doubleValue; // 浮点数形式的值 / Value in double form

        /**
         * 构造游戏规则值容器<br>
         * Constructor for game rule value container
         *
         * @param stringValue 字符串值 / String value
         * @param booleanValue 布尔值 / Boolean value
         * @param intValue 整数值 / Integer value
         * @param doubleValue 浮点值 / Double value
         */
        public GameruleValue(String stringValue, boolean booleanValue, int intValue, double doubleValue) {
            this.stringValue = stringValue;
            this.booleanValue = booleanValue;
            this.intValue = intValue;
            this.doubleValue = doubleValue;
        }

        @Override
        public String toString() {
            return String.format("String: %s, Boolean: %b, Int: %d, Double: %.2f",
                    stringValue, booleanValue, intValue, doubleValue);
        }

        /**
         * <p>
         *     获取最合适的数据类型表示<br>
         *     优先级：整数 > 浮点数 > 布尔值 > 字符串
         * </p>
         * <p>
         *     Get the most appropriate data type representation<br>
         *     Priority: integer > double > boolean > string
         * </p>
         * @return 最合适的类型值（int, double, boolean, string）/ Most appropriate type value (int, double, boolean, string)
         */
        public Object getOptimalValue() {
            if (stringValue.matches("-?\\d+")) {
                return intValue;
            }
            else if (stringValue.matches("-?\\d+\\.\\d+")) {
                return doubleValue;
            }
            else if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
                return booleanValue;
            }
            else {
                return stringValue;
            }
        }
    }

    /**
     * <p>
     *     获取用于读取游戏规则的世界对象。<br>
     *     优先返回集成服务端的主世界（WorldServer），因为客户端 WorldClient 的 GameRules<br>
     *     仅是由 {@code new WorldInfo(settings, "MpServer")} 构造的原版默认值副本，<br>
     *     且服务端从不向客户端同步整套规则，模组新增的规则只存在于服务端世界。<br>
     *     无集成服务端（如连接远程服务器）时回退到客户端世界副本。
     * </p>
     * <p>
     *     Get the world object used for reading game rules.<br>
     *     Prefer the integrated server's overworld (WorldServer), because the client<br>
     *     WorldClient's GameRules is only a vanilla-default copy built by<br>
     *     {@code new WorldInfo(settings, "MpServer")}, and the server never syncs the<br>
     *     full rule set to the client; mod-added rules exist solely in the server world.<br>
     *     Falls back to the client world copy when there is no integrated server
     *     (e.g. connected to a remote server).
     * </p>
     * @return 用于读取规则的世界，若无可用世界则返回null / World for reading rules, null if none available
     */
    public static World getRuleSourceWorld() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return null;
        }

        // 优先集成服务端主世界（包含模组新增的规则）
        // Prefer the integrated server's overworld (contains mod-added rules)
        if (mc.getIntegratedServer() != null
                && mc.getIntegratedServer().worlds != null
                && mc.getIntegratedServer().worlds.length > 0
                && mc.getIntegratedServer().worlds[0] != null) {
            return mc.getIntegratedServer().worlds[0];
        }

        // 兜底：客户端世界副本（仅含原版默认规则）
        // Fallback: client world copy (vanilla defaults only)
        return mc.world;
    }

    /**
     * 获取所有游戏规则及对应所有类型的值<br>
     * Get all game rules with values of all types
     * @param world 世界对象 / World object
     * @return 包含所有游戏规则名称和完整值的映射 / Map containing all game rule names and complete values
     */
    public static Map<String, GameruleValue> getAllGamerules(World world) {
        Map<String, GameruleValue> gamerules = new HashMap<>();

        if (world == null) {
            LOGGER.warn("World object is null, returning empty gamerule map");
            return gamerules;
        }

        GameRules gameRules = world.getGameRules();
        String[] ruleNames = gameRules.getRules();

        for (String ruleName : ruleNames) {
            GameruleValue value = getGamerule(world, ruleName);
            if (value != null) {
                gamerules.put(ruleName, value);
            }
        }

        LOGGER.debug("Retrieved {} gamerules from world", gamerules.size());
        return gamerules;
    }

    /**
     * 获取特定游戏规则的完整值<br>
     * Get the complete value of specific game rule
     * @param world 世界对象 / World object
     * @param ruleName 规则名称 / Rule name
     * @return 完整的游戏规则值，若规则不存在则返回null / Complete game rule value, null if rule does not exist
     */
    public static GameruleValue getGamerule(World world, String ruleName) {
        if (world == null) {
            LOGGER.warn("World object is null, cannot get gamerule: {}", ruleName);
            return null;
        }

        if (!world.getGameRules().hasRule(ruleName)) {
            LOGGER.debug("Gamerule does not exist: {}", ruleName);
            return null;
        }

        GameRules gameRules = world.getGameRules();
        String stringValue = gameRules.getString(ruleName);
        boolean booleanValue = gameRules.getBoolean(ruleName);

        int intValue = 0;
        double doubleValue = 0.0;

        try {
            Field field = GameRules.class.getDeclaredField("theGameRules");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            TreeMap<String, Object> rulesMap = (TreeMap<String, Object>) field.get(gameRules);

            Object valueObj = rulesMap.get(ruleName);
            if (valueObj != null) {

                Field intField = valueObj.getClass().getDeclaredField("valueInteger");
                Field doubleField = valueObj.getClass().getDeclaredField("valueDouble");

                intField.setAccessible(true);
                doubleField.setAccessible(true);

                intValue = intField.getInt(valueObj);
                doubleValue = doubleField.getDouble(valueObj);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to retrieve numeric values for gamerule {} via reflection: {}", ruleName, e.getMessage());
            try {
                intValue = Integer.parseInt(stringValue);
            } catch (NumberFormatException e1) {
                // Keep default value 0
            }
            try {
                doubleValue = Double.parseDouble(stringValue);
            } catch (NumberFormatException e2) {
                // Keep default value 0.0
            }
        }

        return new GameruleValue(stringValue, booleanValue, intValue, doubleValue);
    }

    /**
     * 设置游戏规则值<br>
     * Set game rule value
     * @param world 世界对象 / World object
     * @param ruleName 规则名称 / Rule name
     * @param value 新值 / New value
     * @return 若设置成功则返回true / True if setting is successful
     */
    public static boolean setGamerule(World world, String ruleName, Object value) {
        if (world == null) {
            LOGGER.warn("World object is null, cannot set gamerule: {}", ruleName);
            return false;
        }

        try {
            // 转换值为字符串形式（与GameRules存储一致）
            // Convert value to the string form (consistent with GameRules storage)
            String stringValue;
            if (value instanceof Boolean) {
                stringValue = value.toString();
            } else if (value instanceof Integer) {
                stringValue = value.toString();
            } else if (value instanceof Double) {
                stringValue = value.toString();
            } else if (value instanceof String) {
                stringValue = (String) value;
            } else {
                stringValue = String.valueOf(value);
            }

            world.getGameRules().setOrCreateGameRule(ruleName, stringValue);
            LOGGER.debug("Successfully set gamerule {} to value: {}", ruleName, stringValue);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to set gamerule {} to value {}: {}", ruleName, value, e.getMessage());
            return false;
        }
    }

    /**
     * 添加新的游戏规则<br>
     * Add new game rule
     * @param world 世界对象 / World object
     * @param ruleName 规则名称 / Rule name
     * @param defaultValue 默认值 / Default value
     * @return 若添加成功则返回true / True if addition is successful
     */
    public static boolean addGamerule(World world, String ruleName, Object defaultValue) {
        if (world == null) {
            LOGGER.warn("World object is null, cannot add gamerule: {}", ruleName);
            return false;
        }

        if (world.getGameRules().hasRule(ruleName)) {
            LOGGER.debug("Gamerule already exists: {}", ruleName);
            return false;
        }

        try {
            String stringValue = String.valueOf(defaultValue);
            world.getGameRules().setOrCreateGameRule(ruleName, stringValue);
            LOGGER.debug("Successfully added new gamerule {} with default value: {}", ruleName, stringValue);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to add gamerule {} with default value {}: {}", ruleName, defaultValue, e.getMessage());
            return false;
        }
    }

    /**
     * 检查游戏规则是否存在<br>
     * Check if game rule exists
     * @param world 世界对象 / World object
     * @param ruleName 规则名称 / Rule name
     * @return 若存在则返回true / True if exists
     */
    public static boolean hasGamerule(World world, String ruleName) {
        boolean exists = world != null && world.getGameRules().hasRule(ruleName);
        LOGGER.debug("Gamerule {} exists: {}", ruleName, exists);
        return exists;
    }

    /**
     * 获取所有游戏规则的最佳类型表示<br>
     * Get optimal type representation for all game rules
     * @param world 世界对象 / World object
     * @return 包含规则名称和最佳类型值的映射 / Map containing rule names and optimal type values
     */
    public static Map<String, Object> getOptimalGameruleValues(World world) {
        Map<String, Object> result = new HashMap<>();
        Map<String, GameruleValue> allGamerules = getAllGamerules(world);

        for (Map.Entry<String, GameruleValue> entry : allGamerules.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getOptimalValue());
        }

        LOGGER.debug("Retrieved optimal values for {} gamerules", result.size());
        return result;
    }

    /**
     * 打印所有游戏规则（用于调试）<br>
     * Log all game rules (for debugging)
     * @param world 世界对象 / World object
     */
    public static void logAllGamerules(World world) {
        Map<String, GameruleValue> gamerules = getAllGamerules(world);

        LOGGER.info("=== All Game Rules (Complete Information) ===");
        for (Map.Entry<String, GameruleValue> entry : gamerules.entrySet()) {
            LOGGER.info("{}: {}", entry.getKey(), entry.getValue());
        }
        LOGGER.info("=============================================");

        LOGGER.info("=== All Game Rules (Optimal Types) ===");
        Map<String, Object> optimalValues = getOptimalGameruleValues(world);
        for (Map.Entry<String, Object> entry : optimalValues.entrySet()) {
            LOGGER.info("{}: {} ({})", entry.getKey(), entry.getValue(), entry.getValue().getClass().getSimpleName());
        }
        LOGGER.info("======================================");
    }
}