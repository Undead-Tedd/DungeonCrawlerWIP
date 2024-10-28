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
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "UnexpectedScenarioHandler")
public class UnexpectedScenarioHandlerTask extends BranchTask {

    private final DungeonCrawler bot;

    public UnexpectedScenarioHandlerTask(DungeonCrawler bot) {
        this.bot = bot;
    }

    @Override
    public boolean validate() {
        Player player = Players.getLocal();

        boolean isPlayerLowHealth = player != null && Health.getCurrentPercent() <= 30;
        boolean isPlayerDead = player != null && Health.getCurrentPercent() == 0;
        boolean isInStartingRoom = isPlayerInStartingRoom();
        boolean isInventoryFull = DungeonUtils.isInventoryFull() && DungeonUtils.shouldDropPaddlefishForLoot();

        log.info("Validating Unexpected Scenario - Player is dead: {}, Low health: {}, In Starting Room: {}, Inventory Full: {}",
                isPlayerDead, isPlayerLowHealth, isInStartingRoom, isInventoryFull);

        return isPlayerDead || isPlayerLowHealth || isInStartingRoom || isInventoryFull;
    }

    @Override
    public TreeTask successTask() {
        if (DungeonUtils.isInventoryFull() && DungeonUtils.shouldDropPaddlefishForLoot()) {
            bot.updateTaskStatus("Inventory is full, dropping paddlefish to make space for loot...");
            dropPaddlefish();
            return this;
        }

        bot.updateTaskStatus("Handling post-boss or recovery actions...");
        return new PostBossManagementTask(bot);
    }

    @Override
    public TreeTask failureTask() {
        return null;
    }

    // New method to drop paddlefish
    private void dropPaddlefish() {
        log.info("Dropping one raw paddlefish to make space for high-priority loot.");
        var paddlefish = com.runemate.game.api.hybrid.local.hud.interfaces.Inventory.getItems("Raw Paddlefish").first();
        if (paddlefish != null && paddlefish.interact("Drop")) {
            Execution.delay(300, 600);
            log.info("Successfully dropped a raw paddlefish.");
        } else {
            log.warn("Failed to drop paddlefish or none found.");
        }
    }

    // Check if the player is in the starting room (Bryn or Reward Chest nearby)
    private boolean isPlayerInStartingRoom() {
        Npc bryn = Npcs.newQuery().names("Bryn").results().first();
        if (bryn != null) {
            log.info("Bryn detected in the room.");
            bot.updateTaskStatus("Bryn detected in the starting room.");
            return true;
        }

        GameObject rewardChest = GameObjects.newQuery().names("Reward Chest").results().first();
        if (rewardChest != null) {
            log.info("Reward Chest detected in the room.");
            bot.updateTaskStatus("Reward Chest detected in the starting room.");
            return true;
        }

        return false;
    }

    private void handleLowHealthScenario() {
        if (DungeonUtils.shouldHeal()) {
            log.info("Player health is low. Healing...");
            DungeonUtils.restoreHealth();
        } else {
            log.warn("Player has low health but no food available!");
        }
    }

    private void handleDeathScenario() {
        GameObject rewardChest = GameObjects.newQuery().names("Reward Chest").results().first();
        if (rewardChest != null) {
            log.info("Looting reward chest after death.");
            bot.updateTaskStatus("Looting reward chest after death...");

            if (rewardChest.interact("Take")) {
                Execution.delay(2000);
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

        restartGauntletRun();
    }

    private void restartGauntletRun() {
        log.info("Attempting to re-enter the Gauntlet...");
        if (DungeonUtils.enterGauntlet(bot.isEnterCorrupted())) {
            bot.updateTaskStatus("Re-entering the Gauntlet...");
        } else {
            log.warn("Failed to re-enter the Gauntlet.");
            bot.updateTaskStatus("Failed to re-enter the Gauntlet.");
        }
    }
}
