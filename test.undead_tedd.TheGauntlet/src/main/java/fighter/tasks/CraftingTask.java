package fighter.tasks;

import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.GroundItems;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.script.framework.tree.LeafTask;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "CraftingTask")
public class CraftingTask extends LeafTask {
    private static final String RAW_PADDLEFISH = "Raw paddlefish";
    private static final String WEAPON_FRAME = "Weapon Frame";
    private static final String CRYSTAL_SHARDS = "Crystal Shards";
    private static final String GRYM_LEAF = "Grym Leaf";
    private static final String VIAL = "Vial";
    private static final String CORRUPTED_SHARDS = "Corrupted Shards";
    private static final String WATER_VIAL = "Water-filled vial";
    private static final String DUST = "Crystal Dust";
    private static final String CORRUPTED_DUST = "Corrupted Dust";
    private static final String PESTLE_MORTAR = "Pestle and mortar"; // Required for making Egniol potions
    // Required materials for crafting armor
    private static final String PHREN_BARK = "Phren Bark";
    private static final String CRYSTAL_ORE = "Crystal Ore";
    private static final String LINUM_TIRINIUM = "Linum Tirinium";
    // Required resource counts for crafting
    private static final int REQUIRED_DUST = 3;
    private static final int MIN_PADDLEFISH = 15;
    private static final int REQUIRED_SHARDS = 160;
    private static final int REQUIRED_VIALS = 3;
    private static final int REQUIRED_WEAPON_SHARDS = 220;
    private static final int REQUIRED_BARK = 3;  // Required Phren Bark
    private static final int REQUIRED_ORE = 3;   // Required Crystal Ore
    private static final int REQUIRED_TIRINIUM = 3;  // Required Linum Tirinium
    private final DungeonCrawler bot;

    public CraftingTask(DungeonCrawler dungeonCrawler) {
        this.bot = dungeonCrawler;
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


    public void executePhase1() {
        bot.updateTaskStatus("Crafting items - Phase 1...");
        boolean weaponCrafted = false;

        // Step 1: check resources and craft weapons
        if (shouldCraftWeapon("Basic", "Crystalline")) {
            bot.updateTaskStatus("Crafting a Crystalline Staff (Basic)...");
            weaponCrafted = craftWeapon("Basic", "Crystalline", "staff");
            bot.updateTaskStatus("Crafting a Crystalline Bow (Basic)...");
            weaponCrafted = weaponCrafted && craftWeapon("Basic", "Crystalline", "bow");
            bot.updateTaskStatus("Crafting a Crystalline Staff (Attuned)...");
            weaponCrafted = weaponCrafted && craftWeapon("Attuned", "Crystalline", "staff");
            bot.updateTaskStatus("Crafting a Crystalline Bow (Attuned)...");
            weaponCrafted = weaponCrafted && craftWeapon("Attuned", "Crystalline", "bow");
        }
        if (shouldCraftWeapon("Basic", "Corrupted")) {
            bot.updateTaskStatus("Crafting a Corrupted Staff (Basic)...");
            weaponCrafted = craftWeapon("Basic", "Corrupted", "staff");
            bot.updateTaskStatus("Crafting a Corrupted Bow (Basic)...");
            weaponCrafted = weaponCrafted && craftWeapon("Basic", "Corrupted", "bow");
            bot.updateTaskStatus("Crafting a Corrupted Staff (Attuned)...");
            weaponCrafted = weaponCrafted && craftWeapon("Attuned", "Corrupted", "staff");
            bot.updateTaskStatus("Crafting a Corrupted Bow (Attuned)...");
            weaponCrafted = weaponCrafted && craftWeapon("Attuned", "Corrupted", "bow");
        }

        // Step 2: Craft basic items and armor
        if (weaponCrafted) {
            craftArmorIfNeeded();
            craftDustAndFillVials();
            makePotionsIfAvailable();
            equipCraftedItems();
            dropUnusedMaterials();
            bot.updateTaskStatus("Phase 1 crafting complete. Advancing to Phase 2...");
            bot.advanceCraftingPhase();
        } else if (canCraftArmor()) {
            bot.updateTaskStatus("Crafting armor...");
            craftArmor();
        } else if (canMakePotions()) {
            bot.updateTaskStatus("Making Egniol potions...");
            makePotions();
        } else {
            bot.updateTaskStatus("Phase 1 crafting complete. Advancing to Phase 2...");
            bot.advanceCraftingPhase();
        }
    }

    private void craftArmor() {
    }

    private boolean canMakePotions() {
        return Inventory.contains(WATER_VIAL) && Inventory.contains(GRYM_LEAF) &&
                (Inventory.contains(DUST) || Inventory.contains(CORRUPTED_DUST));
    }
    private void makePotions() {
        if (canMakePotions()) {
            log.info("Making Egniol potions...");
            bot.updateTaskStatus("Making Egniol potions...");
            for (int i = 0; i < 3; i++) {
                Inventory.getItems(WATER_VIAL).first().interact("Use");
                Inventory.getItems(GRYM_LEAF).first().interact("Use");
                if (Inventory.contains(DUST)) {
                    Inventory.getItems(DUST).first().interact("Use");
                } else {
                    Inventory.getItems(CORRUPTED_DUST).first().interact("Use");
                }
                Execution.delay(2000, 3000);
            }
        } else {
            log.warn("Not enough ingredients to make potions.");
            bot.updateTaskStatus("Not enough ingredients to make potions.");
        }
    }

    private boolean canCraftArmor() {
    }

    private void dropUnusedMaterials() {
    }

    private void equipCraftedItems() {
    }

    private void makePotionsIfAvailable() {
    }

    private void craftDustAndFillVials() {
    }

    private void craftArmorIfNeeded() {
    }

    private boolean shouldCraftWeapon(String variant) {
        return (Inventory.getQuantity(CRYSTAL_SHARDS) >= REQUIRED_WEAPON_SHARDS || Inventory.getQuantity(CORRUPTED_SHARDS) >= REQUIRED_WEAPON_SHARDS) && Inventory.contains(WEAPON_FRAME);
    }
    private boolean craftWeapon(String type, String variant, String itemType) {
        if (shouldCraftWeapon(type, variant)) {
            if (openSingingBowl()) {
                char shortcut = getShortcutForItem(itemType, variant);
                for (int i = 0; i < getCraftCount(itemType); i++) {
                    if (Keyboard.pressKey(shortcut)) {
                        log.info("Crafted a " + variant + " " + itemType + " (" + type + ") using keyboard shortcut.");
                        Execution.delay(600, 1000);  // Wait for crafting to complete
                    } else {
                        log.warn("Failed to craft " + itemType + ": Shortcut key press failed.");
                        return false;
                    }
                }
                return true;
            } else {
                log.warn("Failed to open Singing Bowl interface.");
            }
        } else {
            log.warn("Insufficient resources to craft " + itemType + " (" + type + ").");
        }
        return false;
    }

    private boolean openSingingBowl() {
        GameObject singingBowl = GameObjects.newQuery().names("Singing Bowl").results().first();
        if (singingBowl != null) {
            if (singingBowl.interact("Sing-crystal")) {
                log.info("Opened Singing Bowl interface.");
                Execution.delay(300, 500);
                return true;
            } else {
                log.warn("Failed to interact with Singing Bowl.");
            }
        } else {
            log.warn("Singing Bowl not found.");
        }
        return false;
    }

    private int getCraftCount(String itemType) {
        switch (itemType.toLowerCase()) {
            case "staff":
            case "bow":
                return 2; // Craft twice
            case "vial":
                return 3; // Craft three times
            default:
                return 1; // Default to crafting once
        }
    }

    private char getShortcutForItem(String itemType, String variant) {
        private char getShortcutForItem(String itemType, String variant) {
            switch (itemType.toLowerCase()) {
                case "staff":
                    return variant.equalsIgnoreCase("Crystalline") ? '7' : '7'; // Both use '7' for simplicity
                case "bow":
                    return variant.equalsIgnoreCase("Crystalline") ? '8' : '8'; // Both use '8' for simplicity
                case "helm":
                    return '3'; // Single key for helms
                case "body":
                    return '4'; // Single key for bodies
                case "legs":
                    return '5'; // Single key for legs
                default:
                    log.warn("Invalid item type: " + itemType);
                    return '-';
            }
    }


    private void pickUpDroppedResources() {
        bot.updateTaskStatus("Picking up dropped armor resources...");
        GroundItems.newQuery().names("Crystal Ore", "Phren Bark", "Linum Tirinium").results().forEach(item -> {
            item.interact("Take");
            Execution.delay(200, 400); // Delay between each pick-up
        });
    }

    private void craftAdvancedItems() {
        // Logic for phase 2 crafting with the Singing Bowl
        log.info("Using Singing Bowl to craft perfected weapons and armor.");
        // Placeholder logic for advanced crafting
    }

    private void equipCraftedItems() {
        // Logic to equip newly crafted items
    }

    private void manageInventory() {
        // Logic to manage inventory, drop unnecessary items
    }

    private void collectAndCookPaddlefish() {
        // Logic to collect and cook paddlefish
    }




    private void craftAdvancedItems() {
        bot.updateTaskStatus("Using Singing Bowl to craft perfected weapons and armor...");
        log.info("Using Singing Bowl to craft perfected weapons and armor.");

        // Ensure all advanced items are crafted
        if (DungeonUtils.hasSingingBowl()) {
            craftPerfectedWeapons();
            craftRemainingArmor();
        } else {
            bot.updateTaskStatus("Singing Bowl not found.");
            log.warn("Singing Bowl not found.");
        }
    }

    private void craftPerfectedWeapons() {
        log.info("Crafting perfected staff and bow...");
        // Craft perfected staff
        if (Inventory.contains(WEAPON_FRAME) && (Inventory.getQuantity(CRYSTAL_SHARDS) >= REQUIRED_WEAPON_SHARDS || Inventory.getQuantity(CORRUPTED_SHARDS) >= REQUIRED_WEAPON_SHARDS)) {
            if (Inventory.getItems(WEAPON_FRAME).first().interact("Use")) {
                if (Inventory.contains(CRYSTAL_SHARDS)) {
                    Inventory.getItems(CRYSTAL_SHARDS).first().interact("Use");
                } else {
                    Inventory.getItems(CORRUPTED_SHARDS).first().interact("Use");
                }
                log.info("Crafting perfected staff...");
                Execution.delay(2000, 3000);
            }
        }
        // Craft perfected bow
        if (Inventory.contains(WEAPON_FRAME) && (Inventory.getQuantity(CRYSTAL_SHARDS) >= REQUIRED_WEAPON_SHARDS || Inventory.getQuantity(CORRUPTED_SHARDS) >= REQUIRED_WEAPON_SHARDS)) {
            if (Inventory.getItems(WEAPON_FRAME).first().interact("Use")) {
                if (Inventory.contains(CRYSTAL_SHARDS)) {
                    Inventory.getItems(CRYSTAL_SHARDS).first().interact("Use");
                } else {
                    Inventory.getItems(CORRUPTED_SHARDS).first().interact("Use");
                }
                log.info("Crafting perfected bow...");
                Execution.delay(2000, 3000);
            }
        }
    }

    private void craftRemainingArmor() {
        log.info("Crafting remaining armor...");
        if (canCraftArmor()) {
            if (DungeonUtils.hasSingingBowl()) {
                if (Inventory.getItems(PHREN_BARK).first().interact("Use")) {
                    if (Inventory.contains(CRYSTAL_ORE)) {
                        Inventory.getItems(CRYSTAL_ORE).first().interact("Use");
                    }
                    if (Inventory.contains(LINUM_TIRINIUM)) {
                        Inventory.getItems(LINUM_TIRINIUM).first().interact("Use");
                    }
                    log.info("Crafting armor...");
                    Execution.delay(2000, 3000);
                }
            } else {
                bot.updateTaskStatus("Singing Bowl not found.");
                log.warn("Singing Bowl not found.");
            }
        } else {
            bot.updateTaskStatus("Not enough resources to craft armor.");
            log.warn("Not enough resources to craft armor.");
        }
    }
}