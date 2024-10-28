package fighter.tasks;

import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import fighter.DungeonCrawler;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "PostBossManagementTask")
public class PostBossManagementTask extends LeafTask {

    private final DungeonCrawler bot;
    private static final String REWARD_CHEST = "Reward Chest";
    private static final String BANK_DEPOSIT_BOX = "Bank Deposit Box";
    private static final int MAX_INVENTORY_THRESHOLD = 20; // Number of free spaces needed to avoid depositing

    public PostBossManagementTask(DungeonCrawler bot) {
        this.bot = bot;
    }

    @Override
    public void execute() {
        log.info("Handling post-boss tasks...");
        bot.updateTaskStatus("Managing post-boss actions...");

        // Step 1: Loot the reward chest
        if (lootRewardChest()) {
            // Reset resource tracking after looting the chest
            log.info("Resetting resource tracker for the new run...");
            bot.getResourceTracker().resetForNewRun(); // Reset the resources for the new run

            // Step 2: Check if there are more than 20 free spaces after looting
            int freeSpaces = 28 - Inventory.getQuantity();
            if (freeSpaces < MAX_INVENTORY_THRESHOLD) {
                // Not enough free spaces, deposit items
                log.info("Not enough inventory space after looting, depositing items.");
                bot.updateTaskStatus("Inventory has fewer than 20 spaces, depositing items...");
                depositItems();
            } else {
                // Sufficient free spaces, proceed with the next run
                log.info("Inventory has enough space, skipping deposit and starting the next run.");
                bot.updateTaskStatus("Enough space, starting the next run...");
                // Call the method that initiates the next run or actions after looting.
                startNextRun();
            }
        } else {
            log.warn("No reward chest found or unable to loot.");
            bot.updateTaskStatus("No reward chest found or unable to loot.");
        }
    }

    private boolean lootRewardChest() {
        var rewardChest = GameObjects.newQuery().names(REWARD_CHEST).results().first();
        if (rewardChest != null && rewardChest.interact("Loot")) {
            log.info("Looting reward chest...");
            Execution.delay(1000); // Adjust delay based on looting speed
            return true;
        }
        log.warn("Reward chest not found.");
        return false;
    }

    private void depositItems() {
        var depositBox = GameObjects.newQuery().names(BANK_DEPOSIT_BOX).results().first();
        if (depositBox != null && depositBox.interact("Deposit")) {
            log.info("Depositing items into the bank deposit box...");
            Execution.delayUntil(Inventory::isEmpty, 5000, 10000); // Wait until items are deposited
            log.info("Items deposited successfully.");
        } else {
            log.warn("Bank deposit box not found.");
        }
    }

    private void startNextRun() {
        // Add the logic to start the next run
        // This could involve navigating back to the starting room, resetting tasks, etc.
        log.info("Starting the next run...");
        bot.updateTaskStatus("Preparing for the next run...");
        // Placeholder for starting the next sequence of tasks
        // Example: bot.startNextRunSequence();
    }
}
