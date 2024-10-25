package fighter;

import com.runemate.game.api.hybrid.local.hud.interfaces.Health;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.script.framework.tree.*;
import com.runemate.ui.DefaultUI;
import fighter.tasks.tracking.ResourceTracker;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.Text;
import fighter.tasks.*;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "DungeonCrawler")
public class DungeonCrawler extends TreeBot {
    private DebugConsole debugConsole;
    private DefaultUI ui;
    private Text taskStatusText;
    private boolean startingRoomChecked = false; // Flag to track if starting room task is completed
    private boolean enterCorrupted = false; // Default to false, change based on UI toggle
    private ResourceTracker resourceTracker = new ResourceTracker();
    private int craftingPhase = 1;
    private TreeTask dynamicRootTask;

    public ResourceTracker getResourceTracker() {
        return resourceTracker;
    }

    public boolean isEnterCorrupted() {
        return enterCorrupted;
    }

    public void setEnterCorrupted(boolean enterCorrupted) {
        this.enterCorrupted = enterCorrupted;
    }

    public boolean isStartingRoomChecked() {
        return startingRoomChecked;
    }

    public void setStartingRoomChecked(boolean startingRoomChecked) {
        this.startingRoomChecked = startingRoomChecked;
    }

    public void advanceCraftingPhase() {
        if (craftingPhase == 1) {
            craftingPhase = 2;
            log.info("Advanced to second crafting phase.");
        }
    }

    public void resetAfterCrafting() {
        craftingPhase = 1;
        resourceTracker.reset();
        log.info("Reset crafting progress and resources for new run.");
    }

    public int getCraftingPhase() {
        return craftingPhase;
    }

    @Override
    public void onStart(String... args) {
        // Start the debug console
        debugConsole = new DebugConsole(); // Launch the window
        // Log the start-up message in both RMc and DebugConsole
        log.info("Bot started");
        debugConsole.log("Bot started");
        // Initialize DefaultUI and set up custom panels
        ui = new DefaultUI(this);
        ToggleButton toggleButton = new ToggleButton("Enter Corrupted");
        toggleButton.setSelected(false); // Default to false
        toggleButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            setEnterCorrupted(newValue);
            updateTaskStatus("Set to Enter " + (newValue ? "Corrupted" : "Normal") + " Gauntlet.");
        });
        DefaultUI.addPanel(this, "Settings", toggleButton);
        // Add a runtime panel
        Text runtimeText = new Text("Current bot runtime: ");
        runtimeText.textProperty().bind(ui.runtimeProperty().asString());
        DefaultUI.addPanel(this, "Runtime", runtimeText);
        // Initialize task status text and add the panel for task updates
        taskStatusText = new Text("Initializing...");
        DefaultUI.addPanel(this, "Task Status", taskStatusText);
        // Set initial status
        DefaultUI.setStatus(this, "Initializing Dungeon Crawler");
        // Initially set the task status to show that we're starting
        taskStatusText.setText("Starting the bot...");
    }

    @Override
    public void onStop() {
        log.info("Bot stopped");
        debugConsole.log("Bot stopped");
        debugConsole.dispose();
    }

    // Add this getter for DebugConsole
    public DebugConsole getDebugConsole() {
        return debugConsole;
    }

    // Method to dynamically update task status
    public void updateTaskStatus(String status) {
        log.info("Task Status Updated: " + status);
        debugConsole.log("Task Status Updated: " + status);
        taskStatusText.setText(status);
    }

    // Method to dynamically set tasks
    public void setTask(LeafTask task) {
        this.dynamicRootTask = task;  // Sets dynamic root task
    }

    @Override
    public TreeTask createRootTask() {
        log.info("Creating root task for DungeonCrawler");

        // Check if dynamic task is set; if so, prioritize it as root task
        if (dynamicRootTask != null) {
            return dynamicRootTask;
        }

        // If no dynamic task, proceed with default logic
        if (!isStartingRoomChecked() && !DungeonUtils.isInStartingRoom()) {
            log.info("Not in the starting room. Searching...");
            updateTaskStatus("Searching for starting room...");
            return new StartingRoomCheckTask(this);
        }

        // Default RootTask if no conditions matched
        return new RootTask();
    }

    private class RootTask extends BranchTask {
        private final TreeTask startingRoomCheckTask = new StartingRoomCheckTask(DungeonCrawler.this);
        private final TreeTask combatTask = new CombatTask(DungeonCrawler.this);
        private final TreeTask resourceGatheringTask = new ResourceGatheringTask(DungeonCrawler.this);
        private final TreeTask craftingTask = new CraftingTask(DungeonCrawler.this);
        private final TreeTask bossFightTask = new BossFightTask(DungeonCrawler.this);
        private final TreeTask postBossManagementTask = new PostBossManagementTask(DungeonCrawler.this);
        private final TreeTask continueNormalOperationsTask = new ContinueNormalOperationsTask(DungeonCrawler.this, new ResourceTracker());
        private final TreeTask unexpectedScenarioHandlerTask = new UnexpectedScenarioHandlerTask(DungeonCrawler.this);

        @Override
        public boolean validate() {
            return Health.getCurrentPercent() > 50; // Simplified health threshold
        }

        @Override
        public TreeTask successTask() {
            if (!isStartingRoomChecked()) {
                updateTaskStatus("Checking starting room...");
                setStartingRoomChecked(true);
                return startingRoomCheckTask;
            }

            if (combatTask.validate()) {
                updateTaskStatus("Fighting weak monsters or demi-bosses...");
                return combatTask;
            }

            if (resourceGatheringTask.validate()) {
                updateTaskStatus("Gathering resources...");
                return resourceGatheringTask;
            }

            if (craftingTask.validate()) {
                updateTaskStatus("Crafting items...");
                return craftingTask;
            }

            if (bossFightTask.validate()) {
                updateTaskStatus("Fighting boss...");
                return bossFightTask;
            }

            if (postBossManagementTask.validate()) {
                updateTaskStatus("Handling post-boss tasks...");
                return postBossManagementTask;
            }

            if (Inventory.getItems().size() > 20) {
                updateTaskStatus("Inventory full, depositing items...");
                return postBossManagementTask;
            }

            if (!bossFightTask.validate() && !postBossManagementTask.validate()) {
                updateTaskStatus("Continuing normal operations...");
                return continueNormalOperationsTask;
            }

            updateTaskStatus("No valid task found. Stopping bot...");
            DungeonCrawler.this.stop();
            return null;
        }

        @Override
        public TreeTask failureTask() {
            updateTaskStatus("Handling unexpected scenario...");
            return unexpectedScenarioHandlerTask;
        }
    }
}
