package fighter.tasks;

import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.region.GroundItems;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import fighter.tasks.tracking.ResourceTracker;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "ResourceGatheringTask")
public class ResourceGatheringTask extends LeafTask {
    private final DungeonCrawler bot;
    private final ResourceTracker tracker;

    public ResourceGatheringTask(DungeonCrawler bot) {
        this.bot = bot;
        this.tracker = bot.getResourceTracker();
    }

    @Override
    public void execute() {
        bot.updateTaskStatus("Gathering resources...");
        log.info("Starting resource gathering task...");

        // Find a resource node based on needed resources
        GameObject resourceNode = DungeonUtils.getNeededResourceNode();
        if (resourceNode != null) {
            String resourceName = resourceNode.getDefinition().getName();
            log.info("Found needed resource node: " + resourceName);
            bot.updateTaskStatus("Gathering resource: " + resourceName);
            if (resourceNode.interact("Gather")) {
                Execution.delay(1000, 2000); // Wait for gathering action to complete
                tracker.trackResource(resourceName); // Track the gathered resource
                Execution.delay(500); // Small delay between actions
            }
        } else {
            log.info("No needed resource nodes found in this room.");
            bot.updateTaskStatus("No needed resource nodes found.");
        }

        // Handle inventory full case
        if (Inventory.isFull()) {
            log.info("Inventory is full, handling resources.");
            handleFullInventory();
        }

        // Handle tool management (drop unnecessary tools)
        manageTools();

        // Collect any loot that is still needed from the ground
        collectNeededLoot();
    }

    /**
     * Manage tools by dropping unnecessary ones once all resources are gathered.
     */
    private void manageTools() {
        // Example logic: Drop tools that are no longer needed
        if (tracker.hasGatheredAll("Pickaxe") && Inventory.contains("Pickaxe")) {
            log.info("Dropping Pickaxe as all ores have been gathered.");
            Inventory.getItems("Pickaxe").first().interact("Drop");
        }
        if (tracker.hasGatheredAll("Hatchet") && Inventory.contains("Hatchet")) {
            log.info("Dropping Hatchet as all wood resources have been gathered.");
            Inventory.getItems("Hatchet").first().interact("Drop");
        }
        if (tracker.hasGatheredAll("Hammer") && Inventory.contains("Hammer")) {
            log.info("Dropping Hammer as all bars have been crafted.");
            Inventory.getItems("Hammer").first().interact("Drop");
        }
    }

    /**
     * Handle full inventory by dropping unneeded items or teleporting back to the spawn.
     */
    private void handleFullInventory() {
        // Drop unnecessary items first
        Inventory.getItems().forEach(item -> {
            log.info("Dropping unnecessary item from the inventory: " + item.getDefinition().getName());
            item.interact("Drop");
        });

        // If still full, teleport back to the starting room to bank/craft
        if (Inventory.isFull()) {
            if (DungeonUtils.hasTeleportCrystal()) {
                log.info("Teleporting back to spawn using Teleport Crystal.");
                DungeonUtils.teleportBackToSpawn();
            } else {
                log.warn("Inventory full and no Teleport Crystal available.");
                bot.updateTaskStatus("Inventory full, no teleport crystal. Stopping task.");
            }
        }
    }

    /**
     * Collect loot that is still needed from the ground.
     */
    private void collectNeededLoot() {
        GroundItems.newQuery().results().forEach(item -> {
            if (DungeonUtils.isNeededLoot(item.getDefinition().getName())) {
                log.info("Looting needed item: " + item.getDefinition().getName());
                item.interact("Take");
                Execution.delay(500, 1000); // Delay for looting action
            }
        });
    }
}
