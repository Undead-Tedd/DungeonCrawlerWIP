package fighter.tasks;

import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import lombok.extern.log4j.Log4j2;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Log4j2(topic = "DungeonTraversalTask")
public class DungeonTraversalTask extends LeafTask {
    private final DungeonCrawler bot;
    private final BossType bossType;
    private final Set<Coordinate> visitedRooms = new HashSet<>(); // Tracks visited rooms

    public DungeonTraversalTask(DungeonCrawler bot, BossType bossType) {
        this.bot = bot;
        this.bossType = bossType;
    }

    @Override
    public void execute() {
        Coordinate currentLocation = bot.getRoomLocation();

        // Step 1: Track visited rooms
        if (currentLocation != null && !visitedRooms.contains(currentLocation)) {
            log.info("Entered a new room at " + currentLocation);
            visitedRooms.add(currentLocation);
        } else if (currentLocation == null) {
            log.warn("Could not retrieve current location. Skipping room check.");
        }

        // Step 2: Check if teleporting is required based on crafting readiness
        if (shouldTeleport()) {
            teleportToCraftingArea();
            return; // Stop further execution for this tick
        }

        // Step 3: Perform tasks within the room
        if (!performRoomTasks()) {
            // Step 4: If no tasks, attempt to light an unlit node for further traversal
            if (lightUnlitNode()) {
                bot.updateTaskStatus("Node lit, moving to the next area...");
            } else {
                log.warn("No lightable nodes found in the vicinity.");
            }
        }
    }

    /**
     * Determines if teleporting is required based on crafting readiness.
     */
    public boolean shouldTeleport() {
        if (!Inventory.contains("Teleport crystal")) {
            return false;
        }

        // Check crafting phase readiness for teleport decision
        if (bot.getCraftingPhase() == 1 && bot.getResourceTracker().isReadyToCraft()) {
            log.info("Ready for Phase 1 crafting, initiating teleport.");
            return true;
        } else if (bot.getCraftingPhase() == 2 && bot.getResourceTracker().isReadyToCraft()) {
            log.info("Ready for Phase 2 crafting, initiating teleport.");
            return true;
        }
        return false;
    }

    /**
     * Teleports to the crafting area using the teleport crystal.
     */
    public void teleportToCraftingArea() {
        if (Inventory.getItems("Teleport crystal").first() != null
                && Objects.requireNonNull(Inventory.getItems("Teleport crystal").first()).interact("Activate")) {
            log.info("Teleporting back to the crafting area.");
            Execution.delayUntil(() -> DungeonUtils.isInCraftingArea(bot.getBossType()), 2000, 3000);
            bot.updateTaskStatus("Teleported to crafting area.");
        } else {
            log.warn("Failed to activate teleport crystal.");
        }
    }


    /**
     * Checks for and performs any necessary tasks within the current room, such as combat or gathering resources.
     * @return true if any tasks were performed, false otherwise.
     */
    private boolean performRoomTasks() {
        // Check if there are needed resources to gather
        if (bot.hasNeededResource()) {
            bot.updateTaskStatus("Gathering resources...");
            new ResourceGatheringTask(bot).execute();
            return true;
        }

        // Check if there are any combat needs
        if (bot.getNeededMonster() != null) {
            bot.updateTaskStatus("Prioritizing combat...");
            bot.getCombatTask().execute();
            return true;
        }

        return false; // No tasks to perform in the current room
    }

    /**
     * Attempts to find and light an unlit node in the current room.
     * @return true if a node was successfully lit, false otherwise.
     */
    private boolean lightUnlitNode() {
        GameObject unlitNode = GameObjects.newQuery().names("Node").actions("Light").results().nearest();

        if (unlitNode != null && unlitNode.interact("Light")) {
            log.info("Lighting node at " + unlitNode.getPosition());
            Execution.delay(3000, 5000);
            return true;
        }
        return false;
    }
}
