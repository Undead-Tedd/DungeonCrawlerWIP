package fighter;

import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.local.hud.interfaces.Health;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.script.framework.tree.*;
import com.runemate.ui.DefaultUI;
import fighter.tasks.*;
import fighter.tasks.tracking.ResourceTracker;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Log4j2(topic = "DungeonCrawler")
public class DungeonCrawler extends TreeBot {
    @Getter
    private static HashMap<Coordinate, Boolean> scannedRooms = new HashMap<>();

    public static boolean newAreaIsAccessible(Coordinate roomLocation) {
        // Check if this room has already been scanned and marked accessible
        if (scannedRooms.getOrDefault(roomLocation, false)) {
            // Room already marked as accessible, skip further checks
            return true;
        }

        // Query for the "Illuminated Symbol" object to confirm new area activation
        GameObject illuminatedSymbol = GameObjects.newQuery().names("Illuminated Symbol").results().first();

        // If the illuminated symbol is found, mark this room as accessible
        if (illuminatedSymbol != null) {
            scannedRooms.put(roomLocation, true); // Store room as accessible
            log.info("New area detected at " + roomLocation + ". Marking room as accessible.");
            return true;
        } else {
            // Room is not accessible yet
            log.warn("Room at " + roomLocation + " is not accessible.");
            return false;
        }

    }

    private DebugConsole debugConsole;
    private Text taskStatusText;
    @Getter
    private boolean botRunning = false;

    // Define instance variables
    @Setter
    @Getter
    private boolean startingRoomChecked = false;

    @Getter
    private boolean enterCorrupted = false;
    @Getter
    private BossType bossType;

    @Getter
    private final ResourceTracker resourceTracker = new ResourceTracker();
    @Getter
    private int craftingPhase = 1;

    // Task declarations
    private final TreeTask craftingTask;
    @Getter
    private final TreeTask combatTask;
    private final TreeTask dungeonTraversalTask;
    private final TreeTask startingRoomCheckTask;
    @Getter
    private final TreeTask bossFightTask;
    @Getter
    private final TreeTask unexpectedScenarioHandlerTask;

    // Lists to track resources and monsters in the current room
    private List<GameObject> currentResources;
    private List<Npc> currentMonsters;

    // Constructor to initialize bossType and tasks
    public DungeonCrawler() {
        // Initialize bossType based on enterCorrupted value
        this.bossType = enterCorrupted ? BossType.CORRUPTED_HUNLLEF : BossType.CRYSTALLINE_HUNLLEF;

        // Initialize tasks after bossType is set
        this.craftingTask = new CraftingTask(this);
        this.combatTask = new CombatTask(this);
        this.dungeonTraversalTask = new DungeonTraversalTask(this, bossType);
        this.startingRoomCheckTask = new StartingRoomCheckTask(this, bossType);
        this.bossFightTask = new BossFightTask(this, bossType);
        this.unexpectedScenarioHandlerTask = new UnexpectedScenarioHandlerTask(this);
    }

    public static void setScannedRooms(HashMap<Coordinate, Boolean> scannedRooms) {
        DungeonCrawler.scannedRooms = scannedRooms;
    }

    // Setter for enterCorrupted to adjust bossType accordingly
    public void setEnterCorrupted(boolean enterCorrupted) {
        this.enterCorrupted = enterCorrupted;
        this.bossType = enterCorrupted ? BossType.CORRUPTED_HUNLLEF : BossType.CRYSTALLINE_HUNLLEF;
    }

    public void advanceCraftingPhase() {
        if (craftingPhase == 1) {
            craftingPhase = 2;
            log.info("Advanced to second crafting phase.");
        }
    }

    public void resetAfterCrafting() {
        resourceTracker.resetForNewRun();
        log.info("Resetting crafting phase and resources for new dungeon run.");
    }

    @Override
    public void onStart(String... args) {
        debugConsole = new DebugConsole();
        log.info("Bot started");
        debugConsole.log("Bot started");

        DefaultUI ui = new DefaultUI(this);

        // Start Bot Toggle Button
        ToggleButton startButton = new ToggleButton("OFF");
        startButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");
        startButton.setSelected(false);
        startButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            botRunning = newValue;
            if (botRunning) {
                startButton.setText("ON");
                startButton.setStyle("-fx-background-color: green; -fx-text-fill: white;");
                log.info("Bot has started.");
                updateTaskStatus("Bot is running.");
            } else {
                startButton.setText("OFF");
                startButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");
                log.info("Bot is stopped.");
                updateTaskStatus("Bot is stopped.");
            }
        });

        // Enter Corrupted Toggle Button
        ToggleButton corruptedButton = new ToggleButton("Enter Corrupted (OFF)");
        corruptedButton.setStyle("-fx-background-color: cyan; -fx-text-fill: black;");
        corruptedButton.setSelected(false);
        corruptedButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                setEnterCorrupted(true);
                corruptedButton.setText("Enter Corrupted (ON)");
                corruptedButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");
                corruptedButton.setDisable(true);  // Disable after being turned on
                updateTaskStatus("Set to Enter Corrupted Gauntlet.");
                log.info("Locked to Enter Corrupted Gauntlet.");
            }
        });

        // Add UI elements to Settings panel
        DefaultUI.addPanel(this, "Settings", startButton);
        DefaultUI.addPanel(this, "Settings", corruptedButton);

        // Runtime Text
        Text runtimeText = new Text("Current bot runtime: ");
        runtimeText.textProperty().bind(ui.runtimeProperty().asString());
        DefaultUI.addPanel(this, "Runtime", runtimeText);

        taskStatusText = new Text("Initializing...");
        DefaultUI.addPanel(this, "Task Status", taskStatusText);

        DefaultUI.setStatus(this, "Initializing Dungeon Crawler");
        taskStatusText.setText("Starting the bot...");
    }

    @Override
    public TreeTask createRootTask() {
        return new ConditionalRootTask();
    }

    public void setStopReason(String reason) {
        log.info("Stopping bot due to: " + reason);
        updateTaskStatus(reason);
        super.stop(reason);
    }

    // Scan the dungeon for new resources and monsters
    public void scanDungeon() {
        currentResources = GameObjects.newQuery().names("Node", "Ore", "Resource").results().asList();  // Adjust names as needed
        currentMonsters = Npcs.newQuery().names("MonsterName1", "MonsterName2", "BossName").results().asList();  // Adjust names as needed
        log.info("Dungeon scanned. Found " + currentResources.size() + " resources and " + currentMonsters.size() + " monsters.");
    }

    public Npc getNeededMonster() {
        for (Npc npc : currentMonsters) {
            if (DungeonUtils.isNeededMonster(npc, resourceTracker)) {
                return npc;
            }
        }
        return null;
    }

    public boolean hasNeededResource() {
        for (GameObject resource : currentResources) {
            String resourceName = Objects.requireNonNull(resource.getDefinition()).getName();
            if (ResourceTracker.isNeededResource(resourceName)) {
                return true;
            }
        }
        return false;
    }


    private class ConditionalRootTask extends BranchTask {
        private final TreeTask rootTask = new RootTask();

        @Override
        public boolean validate() {
            return botRunning;
        }

        @Override
        public TreeTask successTask() {
            return rootTask;
        }

        @Override
        public TreeTask failureTask() {
            return new IdleTask();
        }
    }

    private class RootTask extends BranchTask {
        @Override
        public boolean validate() {
            return Health.getCurrentPercent() > 50;
        }

        @Override
        public TreeTask successTask() {
            // Step 1: Finish starting room check (only executes once)
            if (!isStartingRoomChecked()) {
                updateTaskStatus("Checking starting room...");
                startingRoomChecked = true;
                return startingRoomCheckTask;
            }

            // Step 2: Check if crafting is ready (always checked first in each room)
            if (resourceTracker.isReadyToCraft()) {
                updateTaskStatus("Preparing for crafting...");
                return craftingTask;
            }

            // Step 3: Room scan, prioritizing combat if needed
            scanDungeon();
            Npc target = getNeededMonster();
            if (target != null && combatTask.validate()) {
                updateTaskStatus("Prioritizing combat...");
                return combatTask;
            }

            // Step 4: Gather resources if they are needed
            if (hasNeededResource()) {
                updateTaskStatus("Gathering resources...");
                return new ResourceGatheringTask(DungeonCrawler.this);
            }

            if (((DungeonTraversalTask) dungeonTraversalTask).shouldTeleport()) {
                updateTaskStatus("Teleporting for crafting...");
                ((DungeonTraversalTask) dungeonTraversalTask).teleportToCraftingArea();
                return null;
            }


            // Step 6: Traverse the dungeon if all other tasks are unnecessary
            if (dungeonTraversalTask.validate()) {
                updateTaskStatus("Traversing dungeon...");
                return dungeonTraversalTask;
            }

            // Step 7: Default to handling unexpected scenarios if no valid tasks are found
            log.warn("No valid task found; handling unexpected scenario.");
            updateTaskStatus("No valid task found, handling unexpected scenario...");
            return unexpectedScenarioHandlerTask;
        }

        @Override
        public TreeTask failureTask() {
            updateTaskStatus("Handling unexpected scenario...");
            return unexpectedScenarioHandlerTask;
        }
    }

    public void updateTaskStatus(String status) {
        log.info("Task Status Updated: " + status);
        debugConsole.log("Task Status Updated: " + status);
        taskStatusText.setText(status);
    }

    public Coordinate getRoomLocation() {
        Player localPlayer = Players.getLocal();
        if (localPlayer != null) {
            return localPlayer.getPosition();
        } else {
            log.warn("Unable to retrieve player location.");
            return null;
        }
    }
    private boolean shouldTeleportForCrafting() {
        if (resourceTracker.isReadyToCraft()) {
            // Check if crafting phase 1 is ready and the bot is away from the crafting area
            if (resourceTracker.getCraftingPhase() == 1
                    && !DungeonUtils.isInCraftingArea(this.getBossType())  // Pass bot.getBossType() here
                    && Inventory.contains("Teleport crystal")) {

                log.info("Teleporting back for Phase 1 crafting...");
                return true;
}
            else if (resourceTracker.getCraftingPhase() == 2 && Inventory.contains("Teleport crystal")) {
                log.info("Teleporting back for Phase 2 crafting...");
                return true;
            }
        }
        return false;
    }

}
