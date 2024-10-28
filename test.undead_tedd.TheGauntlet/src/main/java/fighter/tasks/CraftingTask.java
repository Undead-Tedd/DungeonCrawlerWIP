package fighter.tasks;

import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.script.framework.tree.LeafTask;
import fighter.DungeonCrawler;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

@Log4j2(topic = "CraftingTask")
public class CraftingTask extends LeafTask {
    private static final String SINGING_BOWL_NAME = "Singing Bowl";
    private static final String WATER_PUMP_NAME = "Water Pump";
    private static final String RAW_PADDLEFISH = "Raw paddlefish";
    private static final String PESTLE_AND_MORTAR = "Pestle and mortar";
    private static final String GRYM_LEAF = "Grym Leaf";
    private static final String CRYSTAL_DUST = "Crystal Dust";
    private static final String CORRUPTED_DUST = "Corrupted Dust";
    private static final String WATER_FILLED_VIAL = "Water-filled vial";
    private static final String UNF_POTION = "Grym potion(unf)";
    private static final int REQUIRED_VIALS = 3;
    private final DungeonCrawler bot;

    public CraftingTask(DungeonCrawler bot) {
        this.bot = bot;
    }

    @Override
    public void execute() {
        int phase = bot.getCraftingPhase();
        if (phase == 1) {
            executePhase1();
        } else if (phase == 2) {
            executePhase2();
        } else {
            bot.updateTaskStatus("Unknown crafting phase.");
        }
    }

    // Phase 1 Logic
    private void executePhase1() {
        bot.updateTaskStatus("Crafting items - Phase 1...");
        if (openSingingBowl()) {
            craftWeapons();  // Phase 1 weapons
            craftArmor();  // Phase 1 armor
            craftVials();  // 3 vials
            equipCraftedItems();
            fillVials();
        if (!Inventory.contains("Teleport Crystal")) {
            craftTeleportCrystal();
        }
            dropRawPaddlefish();
            makeDust();
            makePotions();
            bot.updateTaskStatus("Phase 1 crafting complete. Advancing to Phase 2...");
            bot.advanceCraftingPhase();
        } else {
            bot.updateTaskStatus("Failed to open Singing Bowl for crafting.");
        }
    }

    // Phase 2 Logic
    private void executePhase2() {
        bot.updateTaskStatus("Crafting items - Phase 2...");
        if (openSingingBowl()) {
            craftPerfectedWeapons();
            craftMissingArmor();
            equipCraftedItems();
            makePotions();
            dropUnwantedItems();
            collectAndCookPaddlefish();
            bot.updateTaskStatus("Phase 2 crafting and inventory cleanup complete.");
        } else {
            bot.updateTaskStatus("Failed to open Singing Bowl for Phase 2 crafting.");
        }
    }

    // Open Singing Bowl
    private boolean openSingingBowl() {
        GameObject singingBowl = GameObjects.newQuery().names(SINGING_BOWL_NAME).results().first();
        if (singingBowl != null && singingBowl.interact("Sing-crystal")) {
            log.info("Opened Singing Bowl interface.");
            Execution.delay(300, 500);
            return true;
        } else {
            log.warn("Failed to interact with Singing Bowl.");
            return false;
        }
    }

    private void craftTeleportCrystal() {
        log.info("Crafting a Teleport Crystal.");
        if (Keyboard.pressKey('1')) {  // Assuming '1' is the key to craft the teleport crystal
            Execution.delay(300, 600); // Wait for the crafting action to complete
            log.info("Teleport Crystal crafted successfully.");
        } else {
            log.warn("Failed to initiate Teleport Crystal crafting.");
        }
    }


    //  Weapons for Phase 1
    private void craftWeapons() {
        log.info("Crafting basic and attuned weapons.");
        pressKeyTwice('8'); // Crystalline Bow
        pressKeyTwice('7'); // Crystalline Staff
        pressKeyTwice('8'); // Corrupted Bow
        pressKeyTwice('7'); // Corrupted Staff
    }

    //  Armor for Phase 1
    private void craftArmor() {
        log.info("Crafting available armor.");
        Keyboard.pressKey('3'); // Helm
        Execution.delay(300, 600);
        Keyboard.pressKey('4'); // Body
        Execution.delay(300, 600);
        Keyboard.pressKey('5'); // Legs
    }

    // Craft Perfected Weapons for Phase 2
    private void craftPerfectedWeapons() {
        log.info("Crafting perfected weapons.");
        Keyboard.pressKey('8'); // Perfected Bow
        Execution.delay(300, 600);
        Keyboard.pressKey('7'); // Perfected Staff
        Execution.delay(300, 600);
    }

    // Craft missing armor for Phase 2 if not done in Phase 1
    private void craftMissingArmor() {
        log.info("Crafting advanced armor pieces if not crafted.");
        Keyboard.pressKey('3'); // Helm
        Execution.delay(300, 600);
        Keyboard.pressKey('4'); // Body
        Execution.delay(300, 600);
        Keyboard.pressKey('5'); // Legs
    }

    // Craft Vials for Phase 1
    private void craftVials() {
        log.info("Crafting 3 vials.");
        pressKeyThrice(); // Vials
    }

    // Equip Crafted Items
    private void equipCraftedItems() {
        log.info("Equipping crafted armor and staff.");
        Inventory.getItems(item -> item.getDefinition() != null &&
                        (item.getDefinition().getName().contains("Bow") ||
                                item.getDefinition().getName().contains("Helm") ||
                                item.getDefinition().getName().contains("Body") ||
                                item.getDefinition().getName().contains("Legs") ||
                                item.getDefinition().getName().contains("Staff")))
                .forEach(item -> {
                    String action = Objects.requireNonNull(item.getDefinition()).getName().contains("Bow") || item.getDefinition().getName().contains("Staff") ? "Wield" : "Wear";
                    item.interact(action);
                    Execution.delay(200, 400);
                });
    }

    // Utility methods for pressing keys
    private void pressKeyTwice(char key) {
        for (int i = 0; i < 2; i++) {
            Keyboard.pressKey(key);
            Execution.delay(300, 600);
        }
    }

    private void pressKeyThrice() {
        for (int i = 0; i < 3; i++) {
            Keyboard.pressKey('6');
            Execution.delay(300, 600);
        }
    }

    // Other supporting methods
    private void dropRawPaddlefish() {
        log.info("Dropping raw paddlefish.");
        Inventory.getItems(RAW_PADDLEFISH).forEach(item -> item.interact("Drop"));
        Execution.delay(200, 400);
    }

    private void dropUnwantedItems() {
        log.info("Dropping unwanted items.");
        Inventory.getItems(item -> !Objects.requireNonNull(item.getDefinition()).getName().contains("Bow") &&
                        !item.getDefinition().getName().contains("Staff") &&
                        !item.getDefinition().getName().contains("Potion") &&
                        !item.getDefinition().getName().equals("Raw paddlefish"))
                .forEach(item -> item.interact("Drop"));
        Execution.delay(200, 400);
    }

    private void makeDust() {
        log.info("Using pestle and mortar on dust.");
        // Logic for making dust
    }

    private void makePotions() {
        log.info("Making potions if ingredients are available.");
        // Logic for making potions
    }

    private void collectAndCookPaddlefish() {
        log.info("Collecting and cooking paddlefish.");
        // Logic for collecting and cooking paddlefish
    }

    private void fillVials() {
        log.info("Filling vials at the water pump.");
        GameObject waterPump = GameObjects.newQuery().names(WATER_PUMP_NAME).results().first();
        if (waterPump != null) {
            waterPump.interact("Fill");
            Execution.delay(3000);
        }
    }
}
