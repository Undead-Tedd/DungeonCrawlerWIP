package fighter.tasks;

import com.runemate.game.api.hybrid.local.hud.interfaces.SpriteItem;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.region.GroundItems;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import fighter.tasks.tracking.ResourceTracker;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

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
            String resourceName = Objects.requireNonNull(resourceNode.getDefinition()).getName();
            log.info("Found needed resource node: " + resourceName);
            bot.updateTaskStatus("Gathering resource: " + resourceName);
            if (resourceNode.interact("Gather")) {
                Execution.delay(1000, 2000); // Wait for gathering action to complete
                tracker.incrementResourceCount(resourceName); // Increment the gathered resource count
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

        // Manage tools based on gathered resources
        manageTools();

        // Collect any loot that is still needed from the ground
        collectNeededLoot();
    }

    /**
     * Manage tools by dropping unnecessary ones once all resources are gathered.
     */
    private void manageTools() {
        if (tracker.isResourceFullyGathered("Crystal Ore") && Inventory.contains("Pickaxe")) {
            log.info("Dropping Pickaxe as all ores have been gathered.");
            Objects.requireNonNull(Inventory.getItems("Pickaxe").first()).interact("Drop");
        }
        if (tracker.isResourceFullyGathered("Phren Bark") && Inventory.contains("Hatchet")) {
            log.info("Dropping Hatchet as all wood resources have been gathered.");
            Objects.requireNonNull(Inventory.getItems("Hatchet").first()).interact("Drop");
        }
        if (tracker.isResourceFullyGathered("Linum Tirinium") && Inventory.contains("Hammer")) {
            log.info("Dropping Hammer as all crafting bars have been used.");
            Objects.requireNonNull(Inventory.getItems("Hammer").first()).interact("Drop");
        }
    }

    /**
     * Handle full inventory by dropping unneeded items or teleporting back to the spawn.
     */
    private void handleFullInventory() {
        int paddlefishToDrop = Math.min(3, Inventory.getQuantity("Raw Paddlefish"));
        int itemsDropped = 0;

        // Drop 1–3 raw paddlefish if available and needed for gathering space
        for (SpriteItem paddlefish : Inventory.getItems("Raw Paddlefish")) {
            if (itemsDropped >= paddlefishToDrop) break; // Limit drops to 1–3
            log.info("Dropping raw paddlefish to make space for gathering.");
            paddlefish.interact("Drop");
            Execution.delay(300, 500); // Small delay between drops
            itemsDropped++;
        }

        // After dropping paddlefish, check if inventory is still full
        if (Inventory.isFull()) {
            if (DungeonUtils.hasTeleportCrystal()) {
                log.info("Teleporting back to spawn using Teleport Crystal due to full inventory.");
                DungeonUtils.teleportBackToSpawn();
            } else {
                log.warn("Inventory is full and no Teleport Crystal is available.");
                bot.updateTaskStatus("Inventory full, no teleport crystal. Stopping task.");
            }
        }
    }

    /**
     * Collect loot that is still needed from the ground.
     */
    private void collectNeededLoot() {
        GroundItems.newQuery().results().forEach(item -> {
            String itemName = Objects.requireNonNull(item.getDefinition()).getName();
            if (ResourceTracker.isNeededResource(itemName) || DungeonUtils.isHighPriorityLoot(itemName)) {
                log.info("Looting needed item: " + itemName);
                if (item.interact("Take")) {
                    Execution.delay(500, 1000); // Delay for looting action
                    tracker.incrementResourceCount(itemName); // Update tracker
                }
            } else {
                log.info("Skipping unnecessary loot: " + itemName);
            }
        });
    }
}
