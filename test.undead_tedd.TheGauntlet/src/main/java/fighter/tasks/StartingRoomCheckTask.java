package fighter.tasks;

import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import com.runemate.pathfinder.api.MouseCamera;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import lombok.extern.log4j.Log4j2;
import com.runemate.game.api.hybrid.local.Camera;

import java.util.Comparator;
import java.util.Objects;

import static fighter.DungeonUtils.newAreaIsAccessible;

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

        while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME && !roomFound) {
            if (checkForStartingRoomElements()) {
                bot.updateTaskStatus("Found Bryn, proceeding...");
                setCameraSettings();
                if (enterGauntlet()) {
                    bot.updateTaskStatus("Successfully entered the Gauntlet.");
                    adjustCameraAwayFromBarrier();
                    log.info("Checking for lightable nodes...");
                    if (lightNodeAtFirstDoorway()) {
                        bot.updateTaskStatus("First node lit, entering normal operations...");
                        roomFound = true; // Mark room as found only after successful node lighting
                        bot.setStartingRoomChecked(true);
                    } else {
                        bot.updateTaskStatus("Failed to light the first node.");
                        break;
                    }
                } else {
                    log.warn("Failed to enter the Gauntlet. Retrying...");
                    Execution.delay(CHECK_INTERVAL);
                }
            } else {
                log.warn("Bryn or Reward Chest not found. Rechecking in 10 seconds...");
                Execution.delay(CHECK_INTERVAL);
            }
        }

        // Error handling if room was not found after retries
        if (!roomFound) {
            log.error("Bryn or Reward Chest not found within the allowed time. Stopping the bot.");
            bot.updateTaskStatus("Error: Bryn or Reward Chest not found. Stopping the bot.");
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
        MouseCamera.turnTo(90, 1.0);
        Camera.setZoomSetting(128, 5);
        Execution.delay(2000, 3000);
        log.info("Camera settings adjusted.");
        bot.updateTaskStatus("Camera settings applied, proceeding...");
        log.info("Proceeding to look for blocked paths...");

        cameraSettingsApplied = true;
    }

    private void adjustCameraAwayFromBarrier() {
        GameObject barrier = GameObjects.newQuery().names("Barrier").results().first();
        if (barrier != null) {
            Coordinate barrierLocation = barrier.getPosition();
            Camera.turnTo(barrierLocation, 180); // Turn away from the barrier
            Execution.delay(500); // Add half-second delay for smooth adjustment
            log.info("Adjusted camera away from barrier at " + barrierLocation);
        } else {
            log.warn("Barrier not found; camera adjustment skipped.");
        }
    }

    public static boolean lightNodeAtFirstDoorway() {
        log.info("Querying for barrier...");
        GameObject barrierObject = GameObjects.newQuery().names("Barrier").results().first();
        if (barrierObject != null) {
            Coordinate barrierPosition = barrierObject.getPosition();

            // Find the farthest node from the barrier
            GameObject farthestNode = GameObjects.newQuery()
                    .names("Node")
                    .actions("Light")
                    .results().stream()
                    .max(Comparator.comparingDouble(node -> Objects.requireNonNull(node.getPosition()).distanceTo(barrierPosition)))
                    .orElse(null);

            // Interact with the farthest node and check if it activates a new room
            if (farthestNode != null && farthestNode.interact("Light")) {
                Execution.delay(3000, 8000);
                log.info("Node successfully lit.");

                for (int i = 0; i < 3; i++) {
                    Coordinate roomLocation = Objects.requireNonNull(Players.getLocal()).getPosition();
                    if (newAreaIsAccessible(roomLocation)) {
                        log.info("New area is accessible.");
                        return true;
                    } else {
                        log.warn("New area is not accessible, retrying...");
                        Execution.delay(2000, 3000);
                    }
                }
            } else {
                log.warn("No lightable nodes found.");
            }
        } else {
            log.warn("No barriers found.");
        }
        return false;
    }
}