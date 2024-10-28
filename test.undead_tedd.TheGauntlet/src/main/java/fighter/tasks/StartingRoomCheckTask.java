package fighter.tasks;

import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import lombok.extern.log4j.Log4j2;
import com.runemate.game.api.hybrid.local.Camera;

import static fighter.DungeonUtils.lightNodeAtDoorway;

@Log4j2(topic = "StartingRoomCheckTask")
public class StartingRoomCheckTask extends LeafTask {
    private final DungeonCrawler bot;
    private static final int MAX_WAIT_TIME = 180000; // 3 minutes
    private static final int CHECK_INTERVAL = 10000; // 10 seconds

    public StartingRoomCheckTask(DungeonCrawler bot, BossType bossType) {
        this.bot = bot;
    }

    @Override
    public void execute() {
        bot.updateTaskStatus("Checking for Bryn...");
        log.info("Checking for Bryn...");
        long startTime = System.currentTimeMillis();
        boolean roomFound = false;
        boolean gauntletEntered = false;

        while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME && !roomFound) {
            if (checkForStartingRoomElements()) {
                bot.updateTaskStatus("Found Bryn, proceeding...");
                gauntletEntered = enterGauntlet();
                if (gauntletEntered) {
                    bot.updateTaskStatus("Successfully entered the Gauntlet.");
                    setCameraSettings();
                    log.info("Checking for lightable nodes...");
                    if (lightNodeAtDoorway(bot)) {
                        bot.updateTaskStatus("First node lit, entering normal operations...");
                        roomFound = true;
                    } else {
                        bot.updateTaskStatus("Failed to light the first node.");
                        break; // Break the loop to avoid infinite attempts
                    }
                } else {
                    log.warn("Failed to enter the Gauntlet. Retrying...");
                    bot.updateTaskStatus("Failed to enter the Gauntlet. Retrying...");
                    Execution.delay(CHECK_INTERVAL); // Wait before retrying
                }
            } else {
                log.warn("Bryn, reward chest, or Singing Bowl not found. Rechecking in 10 seconds...");
                bot.updateTaskStatus("Bryn, reward chest, or Singing Bowl not found. Rechecking...");
                Execution.delay(CHECK_INTERVAL);
            }
        }

        if (!roomFound) {
            log.error("Bryn, reward chest, or Singing Bowl not found within the allowed time. Stopping the bot.");
            bot.updateTaskStatus("Error: Bryn, reward chest, or Singing Bowl not found. Stopping the bot.");
            bot.setStopReason("Error: Not in the starting area. Stopping bot...");
        }
    }


    private boolean startingRoomElementsChecked = false;

    private boolean checkForStartingRoomElements() {
        if (startingRoomElementsChecked) {
            return true; // If already checked, return true
        }

        Npc bryn = Npcs.newQuery().names("Bryn").results().first();
        GameObject rewardChest = GameObjects.newQuery().names("Reward Chest").results().first();

        if (bryn != null && rewardChest != null) {
            startingRoomElementsChecked = true; // Set the flag to true
            return true;
        }
        return false;
    }

    private boolean gauntletEntered = false;

    private boolean enterGauntlet() {
        if (gauntletEntered) {
            return true; // If already entered, return true
        }

        boolean entered = DungeonUtils.enterGauntlet(bot.isEnterCorrupted());
        if (entered) {
            Execution.delay(3000, 5000); // Additional delay to ensure successful entry
            if (!DungeonUtils.isInStartingRoom()) {
                log.info("Successfully entered the Gauntlet.");
                gauntletEntered = true; // Set the flag to true
                return true;
            } else {
                log.warn("Failed to leave the starting room.");
                return false;
            }
        } else {
            log.warn("Could not find the entrance platform or failed to interact.");
            return false;
        }
    }


    private boolean cameraSettingsApplied = false;


    private void setCameraSettings() {
        if (cameraSettingsApplied) {
            return;
        }
        log.info("Setting camera to overhead view and max zoom out.");
        bot.updateTaskStatus("Adjusting camera...");
        Camera.turnTo(90, 1.0);
        Camera.setZoomSetting(128, 5);
        Execution.delay(4000, 5000);
        log.info("Camera settings adjusted.");
        bot.updateTaskStatus("Camera settings applied, proceeding...");
        log.info("Proceeding to look for blocked paths...");

        cameraSettingsApplied = true;
    }
}