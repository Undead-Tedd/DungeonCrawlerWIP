package fighter;

import com.runemate.game.api.hybrid.local.hud.interfaces.Health;
import com.runemate.game.api.script.framework.tree.*;
import com.runemate.ui.DefaultUI;
import fighter.tasks.tracking.ResourceTracker;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.Text;
import fighter.tasks.*;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "DungeonCrawler")
public class DungeonCrawler extends TreeBot {
    private DebugConsole debugConsole;
    private Text taskStatusText;
    private boolean botRunning = false;

    @Getter
    private boolean startingRoomChecked = false;

    @Getter
    private boolean enterCorrupted = false;

    @Getter
    private ResourceTracker resourceTracker = new ResourceTracker();

    @Getter
    private int craftingPhase = 1;

    @Getter
    private BossType bossType = enterCorrupted ? BossType.CORRUPTED_HUNLLEF : BossType.CRYSTALLINE_HUNLLEF;

    @Getter
    private final TreeTask bossFightTask = new BossFightTask(this, getBossType());
    @Getter
    private final TreeTask unexpectedScenarioHandlerTask = new UnexpectedScenarioHandlerTask(this);


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
        super.stop(reason);  // or call the appropriate stop method if one exists in the superclass
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
        private final TreeTask startingRoomCheckTask = new StartingRoomCheckTask(DungeonCrawler.this, bossType);
        private final TreeTask combatTask = new CombatTask(DungeonCrawler.this);
        private final TreeTask resourceGatheringTask = new ResourceGatheringTask(DungeonCrawler.this);
        private final TreeTask dungeonTraversalTask = new DungeonTraversalTask(DungeonCrawler.this, bossType);
        private final TreeTask craftingTask = new CraftingTask(DungeonCrawler.this);
        private final TreeTask bossFightTask = new BossFightTask(DungeonCrawler.this, bossType);
        private final TreeTask postBossManagementTask = new PostBossManagementTask(DungeonCrawler.this);
        private final TreeTask unexpectedScenarioHandlerTask = new UnexpectedScenarioHandlerTask(DungeonCrawler.this);

        @Override
        public boolean validate() {
            return Health.getCurrentPercent() > 50;
        }

        @Override
        public TreeTask successTask() {
            if (!isStartingRoomChecked()) {
                updateTaskStatus("Checking starting room...");
                return startingRoomCheckTask;
            }

            if (combatTask.validate()) {
                updateTaskStatus("Prioritizing combat...");
                return combatTask;
            }

            if (resourceGatheringTask.validate()) {
                updateTaskStatus("Gathering resources...");
                return resourceGatheringTask;
            }

            if (dungeonTraversalTask.validate()) {
                updateTaskStatus("Traversing dungeon...");
                return dungeonTraversalTask;
            }

            if (shouldCraft()) {
                updateTaskStatus("Preparing for crafting...");
                return craftingTask;
            }

            if (craftingPhase == 2 && bossFightTask.validate()) {
                updateTaskStatus("Boss fight preparation...");
                return bossFightTask;
            }

            if (postBossManagementTask.validate()) {
                updateTaskStatus("Post-boss management...");
                return postBossManagementTask;
            }

            log.warn("No valid task found; handling unexpected scenario.");
            updateTaskStatus("No valid task found, handling unexpected scenario...");
            return unexpectedScenarioHandlerTask;
        }

        @Override
        public TreeTask failureTask() {
            updateTaskStatus("Handling unexpected scenario...");
            return unexpectedScenarioHandlerTask;
        }

        private boolean shouldCraft() {
            return resourceTracker.isReadyToCraft();
        }
    }

    public void updateTaskStatus(String status) {
        log.info("Task Status Updated: " + status);
        debugConsole.log("Task Status Updated: " + status);
        taskStatusText.setText(status);
    }
}
