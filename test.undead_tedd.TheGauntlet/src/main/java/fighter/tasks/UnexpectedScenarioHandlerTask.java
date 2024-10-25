package fighter.tasks;

import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.local.hud.interfaces.Health;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.BranchTask;
import com.runemate.game.api.script.framework.tree.TreeTask;
import fighter.DungeonCrawler; // Import the main bot class to update the UI
import fighter.DungeonUtils;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "UnexpectedScenarioHandler")
public class UnexpectedScenarioHandlerTask extends BranchTask {

    private DungeonCrawler bot;

    public UnexpectedScenarioHandlerTask(DungeonCrawler bot) {
        this.bot = bot;
    }

    @Override
    public boolean validate() {
        Player player = Players.getLocal();

        // Player's health percentage check
        boolean isPlayerLowHealth = player != null && Health.getCurrentPercent() <= 30; // Threshold for low health

        // Check if the player is dead
        boolean isPlayerDead = player != null && Health.getCurrentPercent() == 0;

        // Check if we are in the starting room by finding Bryn or the reward chest
        boolean isInStartingRoom = isPlayerInStartingRoom();

        // New logic: check if the inventory is full and paddlefish should be dropped
        boolean isInventoryFull = DungeonUtils.isInventoryFull() && DungeonUtils.shouldDropPaddlefishForLoot();

        log.info("Validating Unexpected Scenario - Player is dead: {}, Low health: {}, In Starting Room: {}, Inventory Full: {}",
                isPlayerDead, isPlayerLowHealth, isInStartingRoom, isInventoryFull);

        // Handle unexpected scenarios: player death, low health, starting room, or inventory full
        return isPlayerDead || isPlayerLowHealth || isInStartingRoom || isInventoryFull;
    }

    @Override
    public TreeTask successTask() {
        // If it's an inventory full scenario, drop paddlefish to make space for loot
        if (DungeonUtils.isInventoryFull() && DungeonUtils.shouldDropPaddlefishForLoot()) {
            bot.updateTaskStatus("Inventory is full, dropping paddlefish to make space for loot...");
            dropPaddlefish();
            return this;  // After dropping, re-validate the task to continue looting
        }

        // Handle post-boss actions or recovery (e.g., teleporting, looting, inventory check, healing)
        bot.updateTaskStatus("Handling post-boss or recovery actions...");
        return new PostBossManagementTask(bot);  // Continue to the HandlePostBossTask
    }

    @Override
    public TreeTask failureTask() {
        // Continue with normal dungeon operations if no unexpected scenario
        bot.updateTaskStatus("Continuing normal operations...");
        return new ContinueNormalOperationsTask(bot);
    }

    // New method to drop paddlefish
    private void dropPaddlefish() {
        log.info("Dropping one raw paddlefish to make space for high-priority loot.");
        // Drop one raw paddlefish to make space
        var paddlefish = com.runemate.game.api.hybrid.local.hud.interfaces.Inventory.getItems("Raw Paddlefish").first();
        if (paddlefish != null) {
            if (paddlefish.interact("Drop")) {
                Execution.delay(300, 600);
                log.info("Successfully dropped a raw paddlefish.");
            } else {
                log.warn("Failed to drop paddlefish.");
            }
        } else {
            log.warn("No paddlefish found to drop!");
        }
    }

    // Check if the player is in the starting room (Bryn or Reward Chest nearby)
    private boolean isPlayerInStartingRoom() {
        // Check for NPC Bryn
        Npc bryn = Npcs.newQuery().names("Bryn").results().first();
        if (bryn != null) {
            log.info("Bryn detected in the room.");
            bot.updateTaskStatus("Bryn detected in the starting room.");
            return true;
        }

        // Check for the Reward Chest
        GameObject rewardChest = GameObjects.newQuery().names("Reward Chest").results().first();
        if (rewardChest != null) {
            log.info("Reward Chest detected in the room.");
            bot.updateTaskStatus("Reward Chest detected in the starting room.");
            return true;
        }

        return false;
    }

    private void handleLowHealthScenario() {
        // Restore health if paddlefish is available
        if (DungeonUtils.shouldHeal()) {
            log.info("Player health is low. Healing...");
            DungeonUtils.restoreHealth();
        } else {
            log.warn("Player has low health but no food available!");
        }
    }

    private void handleDeathScenario() {
        // Check if there is a reward chest that needs to be looted after death
        GameObject rewardChest = GameObjects.newQuery().names("Reward Chest").results().first();
        if (rewardChest != null) {
            log.info("Looting reward chest after death.");
            bot.updateTaskStatus("Looting reward chest after death...");

            // Interact with the chest only once
            if (rewardChest.interact("Take")) {
                Execution.delay(2000);  // Wait for 2 seconds to ensure the action happened
                log.info("Looting successful. Preparing to restart the run.");
                bot.updateTaskStatus("Looting complete. Preparing to re-enter the Gauntlet...");
            } else {
                log.warn("Failed to loot reward chest.");
                bot.updateTaskStatus("Failed to loot reward chest.");
            }
        } else {
            log.warn("No reward chest available to loot after death.");
            bot.updateTaskStatus("No reward chest available after death.");
        }

        // After looting or no chest found, restart the Gauntlet
        restartGauntletRun();
    }

    private void restartGauntletRun() {
        log.info("Attempting to re-enter the Gauntlet...");
        if (DungeonUtils.enterGauntlet()) {
            bot.updateTaskStatus("Re-entering the Gauntlet...");
        } else {
            log.warn("Failed to re-enter the Gauntlet.");
            bot.updateTaskStatus("Failed to re-enter the Gauntlet.");
        }
    }
}
