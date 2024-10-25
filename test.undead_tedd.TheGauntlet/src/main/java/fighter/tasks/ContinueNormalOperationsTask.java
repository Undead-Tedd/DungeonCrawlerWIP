package fighter.tasks;

import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import com.runemate.game.api.hybrid.local.hud.interfaces.SpriteItem;
import fighter.DungeonCrawler; // Import the main bot class to update the UI
import fighter.DungeonUtils;
import fighter.tasks.tracking.ResourceTracker;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "ContinueNormalOperationsTask")
public class ContinueNormalOperationsTask extends LeafTask {

    private DungeonCrawler bot;
    private ResourceTracker resourceTracker;

    public ContinueNormalOperationsTask(DungeonCrawler bot, ResourceTracker resourceTracker) {
        this.bot = bot;
        this.resourceTracker = resourceTracker;
    }

    @Override
    public void execute() {
        log.info("Continuing normal operations...");
        bot.updateTaskStatus("Continuing normal operations...");

        // Step 1: Prioritize combat if needed
        if (combatNeeded()) {
            Npc target = DungeonUtils.getWeakMonster();
            if (target != null && target.interact("Attack")) {
                log.info("Attacking weak monster: " + target.getName());
                bot.updateTaskStatus("Attacking weak monster: " + target.getName() + "...");
                Execution.delayUntil(() -> target.getHealthGauge() == null, 5000, 10000);
                return; // Exit after engaging in combat
            }

            Npc demiBoss = DungeonUtils.getDemiBoss();
            if (demiBoss != null && demiBoss.interact("Attack")) {
                log.info("Attacking demi-boss: " + demiBoss.getName());
                bot.updateTaskStatus("Attacking demi-boss: " + demiBoss.getName() + "...");
                Execution.delayUntil(() -> demiBoss.getHealthGauge() == null, 5000, 10000);
                return; // Exit after engaging demi-boss combat
            }
        }

        // Step 2: If no combat, prioritize gathering resources
        if (resourceNeeded()) {
            GameObject resourceNode = DungeonUtils.getResourceNode("Crystal Ore", "Linum Tirinium", "Phren Bark");
            if (resourceNode != null && resourceNode.interact("Gather")) {
                log.info("Gathering resource: " + resourceNode.getDefinition().getName());
                bot.updateTaskStatus("Gathering resource: " + resourceNode.getDefinition().getName() + "...");
                Execution.delayUntil(() -> !resourceNode.isVisible(), 5000, 10000);
                return; // Exit after gathering
            }
        }

        // Step 3: Check if it's ready to craft and teleport to spawn
        if (isReadyToCraft()) {
            useTeleportCrystal();
            bot.updateTaskStatus("Teleporting to spawn...");
            Execution.delay(3000, 5000); // Simulate teleport delay

            // Trigger CraftingTask
            GameObject singingBowl = DungeonUtils.getSingingBowl();
            if (singingBowl != null && singingBowl.interact("Craft")) {
                log.info("Using the Singing Bowl to craft.");
                bot.updateTaskStatus("Using the Singing Bowl to craft...");
                Execution.delayUntil(() -> !singingBowl.isVisible(), 5000, 10000);
            }
            return; // Exit after crafting
        }

        // Step 4: Fallback in case nothing is needed
        log.warn("No available tasks found for combat, gathering, or crafting.");
        bot.updateTaskStatus("No available tasks found.");
    }

    // Check if ready for Craft 1
    private boolean isReadyToCraft() {
        // Check if we have the required amount of shards and weapon frames for Craft 1
        return bot.getResourceTracker().getCorruptedShards() >= 150
                && bot.getResourceTracker().getWeaponFrameCount() >= 2;
    }

    // Use teleport crystal to return to spawn
    private void useTeleportCrystal() {
        // Check if a teleport crystal exists in the inventory
        if (DungeonUtils.hasTeleportCrystal()) {
            // Interact with the teleport crystal to teleport back to spawn
            log.info("Using teleport crystal to return to spawn.");
            SpriteItem teleportCrystal = Inventory.getItems("Teleport Crystal").first();
            if (teleportCrystal != null && teleportCrystal.interact("Teleport")) {
                log.info("Teleportation successful.");
                Execution.delay(3000, 5000);  // Wait for teleportation to complete
            } else {
                log.warn("Failed to use teleport crystal.");
            }
        } else {
            log.warn("No teleport crystal found in the inventory.");
        }

        // Step 4: Fallback in case nothing is needed
        log.warn("No available tasks found for combat, gathering, or crafting.");
        bot.updateTaskStatus("No available tasks found.");
    }

    // Helper method to check if combat is still needed (i.e., required monsters)
    private boolean combatNeeded() {
        Npc target = DungeonUtils.getWeakMonster();
        return target != null && DungeonUtils.isNeededMonster(target, bot.getResourceTracker()) && !DungeonUtils.allRequiredMonstersSlain(bot.getResourceTracker());
    }

    // Helper method to check if gathering resources is still needed
    private boolean resourceNeeded() {
        // Assuming isNeededResource is checking for specific resources based on a string and resourceTracker
        return DungeonUtils.isNeededResource("ResourceName") && !bot.getResourceTracker().allResourcesGathered();
    }
}