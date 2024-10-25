package fighter.tasks;

import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import com.runemate.game.api.hybrid.entities.GameObject;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import lombok.extern.log4j.Log4j2;
import com.runemate.game.api.hybrid.local.Camera;

@Log4j2(topic = "StartingRoomCheckTask")
public class StartingRoomCheckTask extends LeafTask {
    private DungeonCrawler bot;
    private static final int MAX_WAIT_TIME = 180000; // 3 minutes in milliseconds
    private static final int CHECK_INTERVAL = 10000; // 10 seconds in milliseconds

    public StartingRoomCheckTask(DungeonCrawler bot) {
        this.bot = bot;
    }

    @Override
    public void execute() {
        bot.updateTaskStatus("Checking for Bryn, reward chest, and Singing Bowl...");
        log.info("Checking for Bryn, reward chest, and Singing Bowl...");
        long startTime = System.currentTimeMillis();
        boolean roomFound = false;
        // Loop to check for Bryn, reward chest, and the Singing Bowl
        while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME) {
            Npc bryn = Npcs.newQuery().names("Bryn").results().first();
            GameObject rewardChest = GameObjects.newQuery().names("Reward Chest").results().first();
            GameObject singingBowl = GameObjects.newQuery().names("Singing Bowl").results().first();
            if (bryn != null && rewardChest != null && singingBowl != null) {
                log.info("Found Bryn, reward chest, and Singing Bowl. Proceeding...");
                bot.updateTaskStatus("Found Bryn, reward chest, and Singing Bowl. Proceeding...");
                // Once found, attempt to enter the Gauntlet
                if (enterGauntlet()) {
                    bot.updateTaskStatus("Successfully entered the Gauntlet.");
                    roomFound = true;
                    // Set the camera settings after entering the Gauntlet
                    setCameraSettings();
                    // Check boss position
                    if (checkBossPosition()) {
                        // Light the first node at the correct door based on the boss position
                        if (lightFirstNode()) {
                            bot.updateTaskStatus("First node lit, entering normal operations...");
                            break;
                        } else {
                            bot.updateTaskStatus("Failed to light the first node.");
                        }
                    }
                } else {
                    log.warn("Failed to enter the Gauntlet. Retrying...");
                    bot.updateTaskStatus("Failed to enter the Gauntlet. Retrying...");
                }
            } else {
                log.warn("Bryn, reward chest, or Singing Bowl not found. Rechecking in 10 seconds...");
                bot.updateTaskStatus("Bryn, reward chest, or Singing Bowl not found. Rechecking...");
                Execution.delay(CHECK_INTERVAL); // Wait for 10 seconds before rechecking
            }
        }
        if (!roomFound) {
            log.error("Bryn, reward chest, or Singing Bowl not found within the allowed time. Stopping the bot.");
            bot.updateTaskStatus("Error: Bryn, reward chest, or Singing Bowl not found. Stopping the bot.");
            bot.stop(); // Stop the bot if not found within 3 minutes
        }
    }

    /**
     * Attempts to enter the Gauntlet by interacting with the entrance platform.
     * @return true if the bot successfully enters the Gauntlet, false otherwise.
     */
    private boolean enterGauntlet() {
        boolean enterCorrupted = bot.isEnterCorrupted();
        GameObject platform = DungeonUtils.getGauntletEntrancePlatform();
        if (platform != null && (enterCorrupted ? platform.interact("Enter-corrupted") : platform.interact("Enter"))) {
            log.info("Entering the Gauntlet through the platform.");
            Execution.delay(2000, 3000); // Wait for the interaction and loading
            // Confirm that the bot is no longer in the starting room
            if (!DungeonUtils.isInStartingRoom()) {
                log.info("Successfully entered the Gauntlet.");
                return true;
            } else {
                log.warn("Failed to enter the Gauntlet. Re-attempting...");
            }
        } else {
            log.warn("Could not find the entrance platform or failed to interact.");
        }
        return false;
    }

    /**
     * Adjust the camera settings for better visibility.
     */
    private void setCameraSettings() {
        log.info("Setting camera to max zoom out and top-down view.");
        bot.updateTaskStatus("Adjusting camera...");
        // Set max zoom
        Camera.setZoom(1.0, 0.05); // Max zoom out
        // Set camera to look down at a 90-degree angle
        Camera.concurrentlyTurnTo(90.0); // Pitch set to top-down view
        log.info("Camera settings adjusted.");
    }

    /**
     * Check boss position to determine which door to open.
     * @return true if the boss position is successfully identified, false otherwise.
     */
    private boolean checkBossPosition() {
        log.info("Checking boss position to determine which door to open...");
        return DungeonUtils.checkBossPosition();
    }

    /**
     * Lights the first node at the door based on the boss's position.
     * @return true if the node was successfully lit, false otherwise.
     */
    private boolean lightFirstNode() {
        GameObject lightNode = DungeonUtils.getLightNodeAtDoor();
        if (lightNode != null && lightNode.interact("Light")) {
            log.info("Lighting the first node at the door.");
            Execution.delay(2000, 3000); // Wait for the interaction and door opening
            return true;
        } else {
            log.warn("Failed to find or interact with the light node at the door.");
        }
        return false;
    }
}
