package fighter.tasks;

import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

@Log4j2(topic = "DungeonTraversalTask")
public class DungeonTraversalTask extends LeafTask {
    private final DungeonCrawler bot;
    private final BossType bossType;

    public DungeonTraversalTask(DungeonCrawler bot, BossType bossType) {
        this.bot = bot;
        this.bossType = bossType;
    }


    @Override
    public void execute() {
        // Check if teleport is needed for crafting phase
        if (bot.getCraftingPhase() == 1 && shouldTeleport()) {
            teleportToCraftingArea();
        } else {
            navigateDungeonRooms();
        }
    }

    /**
     * Determines if teleporting is possible by checking for the teleport crystal in the inventory.
     */
    private boolean shouldTeleport() {
        return Inventory.contains("Teleport crystal");
    }

    /**
     * Handles teleporting to the crafting area using the teleport crystal.
     */
    private void teleportToCraftingArea() {
        if (Objects.requireNonNull(Inventory.getItems("Teleport crystal").first()).interact("Activate")) {
            log.info("Teleporting back to the crafting area.");
            Execution.delayUntil(() -> DungeonUtils.isInCraftingArea(bot.getBossType()), 2000, 3000);
            bot.updateTaskStatus("Teleported to crafting area.");
        } else {
            log.warn("Failed to activate teleport crystal.");
        }
    }


    /**
     * Handles navigating and opening new rooms by finding and lighting nodes.
     */
    private void navigateDungeonRooms() {
        bot.updateTaskStatus("Navigating dungeon rooms...");

        // Select the coordinates based on the boss type (normal or corrupted)
        Coordinate[] northDoorNodes = bossType == BossType.CORRUPTED_HUNLLEF
                ? new Coordinate[]{new Coordinate(1974, 5678, 1), new Coordinate(1977, 5678, 1)}
                : new Coordinate[]{new Coordinate(1910, 5678, 1), new Coordinate(1913, 5678, 1)};

        Coordinate[] eastDoorNodes = bossType == BossType.CORRUPTED_HUNLLEF
                ? new Coordinate[]{new Coordinate(1982, 5673, 1), new Coordinate(1982, 5670, 1)}
                : new Coordinate[]{new Coordinate(1918, 5673, 1), new Coordinate(1918, 5670, 1)};

        Coordinate[] southDoorNodes = bossType == BossType.CORRUPTED_HUNLLEF
                ? new Coordinate[]{new Coordinate(1977, 5665, 1), new Coordinate(1974, 5665, 1)}
                : new Coordinate[]{new Coordinate(1913, 5665, 1), new Coordinate(1910, 5665, 1)};

        Coordinate[] westDoorNodes = bossType == BossType.CORRUPTED_HUNLLEF
                ? new Coordinate[]{new Coordinate(1969, 5670, 1), new Coordinate(1969, 5673, 1)}
                : new Coordinate[]{new Coordinate(1905, 5670, 1), new Coordinate(1905, 5673, 1)};

        if (!attemptToLightNodes(northDoorNodes)) {
            if (!attemptToLightNodes(eastDoorNodes)) {
                if (!attemptToLightNodes(southDoorNodes)) {
                    attemptToLightNodes(westDoorNodes);
                }
            }
        }
    }

    /**
     * Tries to light nodes in a doorway.
     * @param nodes The coordinates of nodes for a given doorway.
     * @return true if successful, false otherwise.
     */
    private boolean attemptToLightNodes(Coordinate[] nodes) {
        for (Coordinate node : nodes) {
            GameObject nodeObject = GameObjects.newQuery().within(nodeAreaAround(node)).names("Node").actions("Light").results().first();
            if (nodeObject != null && nodeObject.interact("Light")) {
                log.info("Lighting node at " + node);
                Execution.delay(2000, 3000);
                return true;
            }
        }
        return false;
    }

    /**
     * Defines a small area around a given coordinate to account for slight inaccuracies.
     * @param node The center coordinate.
     * @return A circular area around the node.
     */
    private Area.Circular nodeAreaAround(Coordinate node) {
        return new Area.Circular(node, 1);
    }
}
