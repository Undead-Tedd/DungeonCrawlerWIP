package fighter.tasks;

import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.definitions.GameObjectDefinition;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import lombok.extern.log4j.Log4j2;
import com.runemate.game.api.hybrid.local.Camera;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Log4j2(topic = "StartingRoomCheckTask")
public class StartingRoomCheckTask extends LeafTask {
    private final DungeonCrawler bot;
    private static final int MAX_WAIT_TIME = 180000; // 3 minutes
    private static final int CHECK_INTERVAL = 10000; // 10 seconds

    // Coordinates for normal and corrupted mode
    private static final Coordinate[] northDoorNodes = {new Coordinate(1910, 5678, 1), new Coordinate(1913, 5678, 1)};
    private static final Coordinate[] eastDoorNodes = {new Coordinate(1918, 5673, 1), new Coordinate(1918, 5670, 1)};
    private static final Coordinate[] southDoorNodes = {new Coordinate(1913, 5665, 1), new Coordinate(1910, 5665, 1)};
    private static final Coordinate[] westDoorNodes = {new Coordinate(1905, 5670, 1), new Coordinate(1905, 5673, 1)};

    private static final Coordinate[] corruptedNorthDoorNodes = {new Coordinate(1974, 5678, 1), new Coordinate(1977, 5678, 1)};
    private static final Coordinate[] corruptedEastDoorNodes = {new Coordinate(1982, 5673, 1), new Coordinate(1982, 5670, 1)};
    private static final Coordinate[] corruptedSouthDoorNodes = {new Coordinate(1977, 5665, 1), new Coordinate(1974, 5665, 1)};
    private static final Coordinate[] corruptedWestDoorNodes = {new Coordinate(1969, 5670, 1), new Coordinate(1969, 5673, 1)};

    // Rectangular areas for normal and corrupted mode
    private static final Area.Rectangular northDoorArea = Area.rectangular(northDoorNodes[0], northDoorNodes[1]);
    private static final Area.Rectangular eastDoorArea = Area.rectangular(eastDoorNodes[0], eastDoorNodes[1]);
    private static final Area.Rectangular southDoorArea = Area.rectangular(southDoorNodes[0], southDoorNodes[1]);
    private static final Area.Rectangular westDoorArea = Area.rectangular(westDoorNodes[0], westDoorNodes[1]);

    private static final Area.Rectangular corruptedNorthDoorArea = Area.rectangular(corruptedNorthDoorNodes[0], corruptedNorthDoorNodes[1]);
    private static final Area.Rectangular corruptedEastDoorArea = Area.rectangular(corruptedEastDoorNodes[0], corruptedEastDoorNodes[1]);
    private static final Area.Rectangular corruptedSouthDoorArea = Area.rectangular(corruptedSouthDoorNodes[0], corruptedSouthDoorNodes[1]);
    private static final Area.Rectangular corruptedWestDoorArea = Area.rectangular(corruptedWestDoorNodes[0], corruptedWestDoorNodes[1]);


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
                    log.info("Checking doorway accessibility...");
                    if (isDoorwayAccessible()) {
                        log.info("Doorway accessible. Attempting to light the first node...");
                        Coordinate[] selectedDoorNodes = determineSelectedDoorNodes();
                        String direction = determineDirection(selectedDoorNodes);
                        if (lightNodeAtDoorway(selectedDoorNodes, direction)) {
                            bot.updateTaskStatus("First node lit, entering normal operations...");
                        } else {
                            bot.updateTaskStatus("Failed to light the first node.");
                        }
                    } else {
                        bot.updateTaskStatus("Doorway not accessible.");
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

    private Coordinate[] determineSelectedDoorNodes() {
        Coordinate[] northNodes = bot.isEnterCorrupted() ? corruptedNorthDoorNodes : northDoorNodes;
        Coordinate[] eastNodes = bot.isEnterCorrupted() ? corruptedEastDoorNodes : eastDoorNodes;
        Coordinate[] southNodes = bot.isEnterCorrupted() ? corruptedSouthDoorNodes : southDoorNodes;
        Coordinate[] westNodes = bot.isEnterCorrupted() ? corruptedWestDoorNodes : westDoorNodes;

        if (isPathBlocked(northNodes, "northSouth")) {
            return southNodes;
        } else if (isPathBlocked(eastNodes, "eastWest")) {
            return westNodes;
        } else if (isPathBlocked(southNodes, "northSouth")) {
            return northNodes;
        } else if (isPathBlocked(westNodes, "eastWest")) {
            return eastNodes;
        } else {
            return northNodes; // Default to north
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

        // Use the updated DungeonUtils method which includes the delay
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

    private boolean doorwayAccessibilityChecked = false;

    private boolean isDoorwayAccessible() {
        if (doorwayAccessibilityChecked) {
            return true;
        }

        log.info("Locating boss position based on missing nodes...");
        Coordinate[] northNodes = bot.isEnterCorrupted() ? corruptedNorthDoorNodes : northDoorNodes;
        Coordinate[] eastNodes = bot.isEnterCorrupted() ? corruptedEastDoorNodes : eastDoorNodes;
        Coordinate[] southNodes = bot.isEnterCorrupted() ? corruptedSouthDoorNodes : southDoorNodes;
        Coordinate[] westNodes = bot.isEnterCorrupted() ? corruptedWestDoorNodes : westDoorNodes;

        log.info("Checking for blocked North path...");
        if (isPathBlocked(northNodes, "northSouth")) {
            log.info("North path blocked. Heading south.");
            doorwayAccessibilityChecked = true;
            return lightNodeAtDoorway(southNodes, "northSouth");
        } else if (isPathBlocked(eastNodes, "eastWest")) {
            log.info("East path blocked. Heading west.");
            doorwayAccessibilityChecked = true;
            return lightNodeAtDoorway(westNodes, "eastWest");
        } else if (isPathBlocked(southNodes, "northSouth")) {
            log.info("South path blocked. Heading north.");
            doorwayAccessibilityChecked = true;
            return lightNodeAtDoorway(northNodes, "northSouth");
        } else if (isPathBlocked(westNodes, "eastWest")) {
            log.info("West path blocked. Heading east.");
            doorwayAccessibilityChecked = true;
            return lightNodeAtDoorway(eastNodes, "eastWest");
        } else {
            log.warn("No blocked path found. Defaulting to north.");
            doorwayAccessibilityChecked = true;
            return lightNodeAtDoorway(northNodes, "northSouth");
        }
    }

    private boolean isPathBlocked(Coordinate[] nodes, String direction) {
        Area.Rectangular areaAroundNodes = DungeonUtils.nodeAreaAround(nodes, direction);
        GameObject nodeObject = GameObjects.newQuery().names("Node").within(areaAroundNodes).results().first();
        if (nodeObject != null && Objects.requireNonNull(nodeObject.getDefinition()).getActions().contains("Light")) {
            log.info("Node with 'Light' action found at one of the nodes in area: " + areaAroundNodes);
            return true;
        }
        log.info("No lightable nodes found in any area, path is accessible.");
        return false;
    }

    private boolean lightNodeAtDoorway(Coordinate[] nodes, String direction) {
        Area.Rectangular areaAroundNodes = DungeonUtils.nodeAreaAround(nodes, direction);
        log.info("Checking lightable nodes in area around coordinates: " + Arrays.toString(nodes) + " with direction: " + direction);

        GameObject nodeObject = GameObjects.newQuery().names("Node").actions("Light").within(areaAroundNodes).results().first();
        if (nodeObject != null) {
            GameObjectDefinition def = nodeObject.getActiveDefinition();
            if (def != null) {
                String[] actions = def.getActions().toArray(new String[0]);
                log.info("Found node. Actions available: " + String.join(", ", actions));
                if (Arrays.asList(actions).contains("Light")) {
                    log.info("Found lightable node at position: " + nodeObject.getPosition() + ". Attempting to interact...");
                    boolean interactionResult = nodeObject.interact("Light");

                    if (interactionResult) {
                        log.info("Node successfully lit.");
                        Execution.delay(3000, 8000); // Ensure interaction completes
                        return true;
                    } else {
                        log.warn("Interaction with light node failed.");
                    }
                } else {
                    log.warn("The 'Light' action is not available on this node.");
                }
            } else {
                log.warn("Node definition is null.");
            }
        } else {
            log.warn("No nodes found in specified area: " + areaAroundNodes);
        }
        return false;
    }




    private String determineDirection(Coordinate[] nodes) {
        Coordinate[] northNodes = bot.isEnterCorrupted() ? corruptedNorthDoorNodes : northDoorNodes;
        Coordinate[] eastNodes = bot.isEnterCorrupted() ? corruptedEastDoorNodes : eastDoorNodes;
        Coordinate[] southNodes = bot.isEnterCorrupted() ? corruptedSouthDoorNodes : southDoorNodes;
        Coordinate[] westNodes = bot.isEnterCorrupted() ? corruptedWestDoorNodes : westDoorNodes;

        if (Arrays.equals(nodes, eastNodes) || Arrays.equals(nodes, westNodes)) {
            return "eastWest";
        } else if (Arrays.equals(nodes, northNodes) || Arrays.equals(nodes, southNodes)) {
            return "northSouth";
        } else {
            log.warn("Invalid nodes provided for determining direction.");
            return "";
        }
    }
}

