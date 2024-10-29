package fighter;

import java.util.HashMap;
import java.util.List;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.GroundItem;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.local.hud.interfaces.*;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.GroundItems;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.osrs.local.hud.interfaces.ControlPanelTab;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.osrs.local.hud.interfaces.Prayer;
import fighter.tasks.BossType;
import fighter.tasks.tracking.ResourceTracker;
import com.runemate.game.api.hybrid.local.hud.interfaces.SpriteItem;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.awt.event.KeyEvent;
import java.util.Objects;

import static fighter.tasks.tracking.ResourceTracker.isNeededResource;

@Log4j2
public class DungeonUtils {

    // Coordinates for normal and corrupted mode
    public static final Coordinate[] northDoorNodes = {new Coordinate(1910, 5678, 1), new Coordinate(1913, 5678, 1)};
    public static final Coordinate[] eastDoorNodes = {new Coordinate(1918, 5673, 1), new Coordinate(1918, 5670, 1)};
    public static final Coordinate[] southDoorNodes = {new Coordinate(1913, 5665, 1), new Coordinate(1910, 5665, 1)};
    public static final Coordinate[] westDoorNodes = {new Coordinate(1905, 5670, 1), new Coordinate(1905, 5673, 1)};

    public static final Coordinate[] corruptedNorthDoorNodes = {new Coordinate(1974, 5678, 1), new Coordinate(1977, 5678, 1)};
    public static final Coordinate[] corruptedEastDoorNodes = {new Coordinate(1982, 5673, 1), new Coordinate(1982, 5670, 1)};
    public static final Coordinate[] corruptedSouthDoorNodes = {new Coordinate(1977, 5665, 1), new Coordinate(1974, 5665, 1)};
    public static final Coordinate[] corruptedWestDoorNodes = {new Coordinate(1969, 5670, 1), new Coordinate(1969, 5673, 1)};

    private static Coordinate currentRoomLocation;

    public static void updateRoomLocation(Coordinate newLocation) {
        currentRoomLocation = newLocation;
    }

    public static Coordinate getRoomLocation() {
        return currentRoomLocation;
    }


    private static final long FLICK_DELAY = 600; // Flick every 600ms

    // Tracks second base return for teleport crystal
    @Getter
    private static boolean secondReturnToBase;

    public static void setSecondReturnToBase(boolean secondReturnToBase) {
        DungeonUtils.secondReturnToBase = secondReturnToBase;
    }

    private static ResourceTracker resourceTracker; // Declare ResourceTracker

    // Get or initialize ResourceTracker instance
    public static ResourceTracker getResourceTracker() {
        if (resourceTracker == null) {
            resourceTracker = new ResourceTracker();
            log.info("Initialized a new ResourceTracker.");
        }
        return resourceTracker;
    }

    /// Check for starting area (Bryn and reward chest)
    public static boolean isInStartingRoom() {
        Npc bryn = Npcs.newQuery().names("Bryn").results().first();
        GameObject rewardChest = GameObjects.newQuery().names("Reward Chest").results().first();
        boolean inStartingRoom = bryn != null && rewardChest != null;

        // Log the result for debugging
        log.debug("Starting room status: " + (inStartingRoom ? "In starting room" : "Not in starting room"));

        return inStartingRoom;
    }

    public static GameObject getGauntletEntrancePlatform() {
        return GameObjects.newQuery().names("The Gauntlet").results().first();
    }

    public static boolean enterGauntlet(boolean enterCorrupted) {
        GameObject platform = getGauntletEntrancePlatform();
        if (platform != null) {
            String interactionType = enterCorrupted ? "Enter-corrupted" : "Enter";
            log.info("Starting our Gauntlet Run (" + (enterCorrupted ? "Corrupted" : "Normal") + "), GL!");
            boolean interactionResult = platform.interact(interactionType);
            if (interactionResult) {
                Execution.delay(3000, 5000); // Adding a delay to ensure interaction completes
            }
            return interactionResult;
        } else {
            log.warn("Gauntlet entry platform is not found.");
            return false;
        }
    }



    // Method to check if we need to heal
    public static boolean shouldHeal() {
        return Health.getCurrentPercent() < 30;
    }

    // Method to check if we need to restore prayer
    public static boolean shouldRestorePrayer() {
        return Prayer.getPoints() < 20;
    }

    // Restore health using Paddlefish
    public static void restoreHealth() {
        var paddlefish = Inventory.getItems("Paddlefish").first();
        if (paddlefish != null) {
            paddlefish.interact("Eat");
            log.info("Eating paddlefish to restore HP.");
            Execution.delay(500, 1000);
        } else {
            log.warn("No paddlefish found in inventory.");
        }
    }

    // Restore prayer using Egniol Potion
    public static void restorePrayer() {
        var egniolPotion = Inventory.getItems(item -> item.getDefinition() != null && item.getDefinition().getName().contains("Egniol Potion")).first();
        if (egniolPotion != null) {
            egniolPotion.interact("Drink");
            log.info("Drinking Egniol Potion to restore prayer/run energy.");
            Execution.delay(500, 1000);
        } else {
            log.warn("We are out of Egniol Potions!");
        }
    }

    // Retrieve resource nodes
    public static GameObject getResourceNode(String... resourceNames) {
        for (String resourceName : resourceNames) {
            GameObject resourceNode = GameObjects.newQuery().names(resourceName).results().first();
            if (resourceNode != null) {
                return resourceNode;
            }
        }
        return null;
    }

    // Main prayer handling logic
    public static void handlePrayerLogic() {
        if (shouldPrayerFlick()) {
            log.info("Flicking prayer due to low prayer points and standing still.");
            switchToPrayerInterface();
            flickPrayer(Prayer.PROTECT_FROM_MELEE);
        } else if (shouldHeal()) {
            log.info("Health low, need to heal.");
            healPlayer();
        } else if (Objects.requireNonNull(Players.getLocal()).isMoving()) {
            log.info("Player is moving, keeping prayer active.");
            keepPrayerActive(Prayer.PROTECT_FROM_MELEE);
        }
    }

    // Decide when to flick prayer
    public static boolean shouldPrayerFlick() {
        return Prayer.getPoints() < 20 && !Objects.requireNonNull(Players.getLocal()).isMoving() && !Inventory.contains("Egniol Potion");
    }

    public static void flickPrayer(Prayer prayer) {
        switchToPrayerInterface();
        while (shouldPrayerFlick()) {
            if (!prayer.isActivated()) {
                prayer.activate();
                log.info("Prayer activated: " + prayer);
            }
            Execution.delay(FLICK_DELAY);
            prayer.deactivate();
            log.info("Prayer deactivated: " + prayer);
            Execution.delay(FLICK_DELAY);
        }
    }

    // Keep prayer active without flicking
    public static void keepPrayerActive(Prayer prayer) {
        if (!prayer.isActivated()) {
            prayer.activate();
            log.info("Activated prayer without flicking: " + prayer);
        }
    }

    // Switch to prayer interface
    public static void switchToPrayerInterface() {
        if (!ControlPanelTab.PRAYER.isOpen()) {
            Keyboard.pressKey(KeyEvent.VK_F1);
            log.info("Switched to prayer interface.");
            Execution.delay(300, 500);
        }
    }

    // Heal the player by eating food
    public static void healPlayer() {
        switchToInventoryInterface();
        restoreHealth();
        switchToPrayerInterface();
    }

    // Switch to Inventory Interface
    private static void switchToInventoryInterface() {
        log.info("Switching to the inventory interface.");
        Keyboard.pressKey(114); // Keycode for F3
        Execution.delay(300, 600);  // Delay to allow the interface to open
        Keyboard.releaseKey(114); // Release F3
    }

    // Crafting logic for Bows and Staffs
    public static boolean isBowNeeded(String tier) {
        return !getResourceTracker().isBowCrafted(tier);
    }

    public static boolean isStaffNeeded(String tier) {
        return !getResourceTracker().isStaffCrafted(tier);
    }

    public static void markBowCrafted(String tier) {
        getResourceTracker().trackBowCrafting(tier);
    }

    public static void markStaffCrafted(String tier) {
        getResourceTracker().trackStaffCrafting(tier);
    }

    public static boolean isLootNeeded() {
        GroundItem loot = GroundItems.newQuery().filter(item -> isNeededResource(Objects.requireNonNull(item.getDefinition()).getName())).results().first();

        if (loot == null) {
            log.info("No needed loot found.");
            return false;
        }

        String lootName = Objects.requireNonNull(loot.getDefinition()).getName();
        log.info("Needed loot found: " + lootName);

        if (Inventory.isFull()) {
            log.info("Inventory is full. Checking if paddlefish should be dropped for high-priority loot.");

            if (isHighPriorityLoot(lootName)) {
                log.info("High-priority loot detected: " + lootName);
                if (shouldDropPaddlefishForLoot()) {
                    dropPaddlefish();
                    return true;
                }
            } else {
                log.info("Loot found, but no space and not high-priority: " + lootName);
                return false;
            }
        }

        return true;
    }

    // Weak monsters include both Crystalline and Corrupted variants
    public static final List<String> WEAK_MONSTER_NAMES = List.of("Crystalline Rat", "Crystalline Spider", "Crystalline Bat",
            "Corrupted Rat", "Corrupted Spider", "Corrupted Bat");

    // Demi-bosses include both Crystalline and Corrupted variants
    public static final List<String> DEMI_BOSS_NAMES = List.of("Crystalline Dragon", "Crystalline Dark Beast",
            "Corrupted Dragon", "Corrupted Dark Beast");

    // Bosses include both Crystal and Corrupted variants of the main boss
    private static final List<String> BOSS_NAMES = List.of("Crystal Hunllef", "Corrupted Hunllef");

    // Check if loot is high priority
    public static boolean isHighPriorityLoot(String lootName) {
        return lootName.equals("Weapon Frame") || lootName.equals("Corrupted Orb") || lootName.equals("Crystalline Orb")
                || lootName.equals("Corrupted Bowstring") || lootName.equals("Crystalline Bowstring");
    }

    // Check if we should drop paddlefish to make space for loot
    public static boolean shouldDropPaddlefishForLoot() {
        return Inventory.contains("Raw Paddlefish") && Inventory.isFull();
    }

    // Drop paddlefish to create space in inventory
    public static void dropPaddlefish() {
        List<SpriteItem> paddlefish = Inventory.getItems("Raw Paddlefish").asList();

        if (!paddlefish.isEmpty()) {
            log.info("Dropping raw paddlefish to make space for high-priority loot.");
            for (SpriteItem fish : paddlefish) {
                if (!Inventory.isFull()) {
                    log.info("Inventory space made. Ready to pick up loot.");
                    break;
                }
                fish.interact("Drop");
                Execution.delay(300, 600);
            }
        } else {
            log.warn("No raw paddlefish in inventory to drop.");
        }
    }

    // Get the nearest weak monster for easy combat
    public static Npc getWeakMonster() {
        return Npcs.newQuery().names(WEAK_MONSTER_NAMES.toArray(new String[0])).results().nearest();
    }

    // Get the nearest demi-boss
    public static Npc getDemiBoss() {
        return Npcs.newQuery().names(DEMI_BOSS_NAMES.toArray(new String[0])).results().nearest();
    }

    // Check if a target is a boss or demi-boss
    public static boolean isBossOrDemiBoss(Npc target) {
        if (target == null) return false;
        String name = target.getName();
        return BOSS_NAMES.contains(name) || DEMI_BOSS_NAMES.contains(name);
    }

    // Determine if an NPC is needed based on resource requirements
    public static boolean isNeededMonster(Npc target, ResourceTracker resourceTracker) {
        if (target == null || resourceTracker == null) return false;
        String name = target.getName();

        // Demi-bosses needed for crafting resources
        if (DEMI_BOSS_NAMES.contains(name)) {
            assert name != null;
            return (name.contains("Dragon") && resourceTracker.isArmorCrafted("body", "corrupted")) ||
                    (name.contains("Dark Beast") && resourceTracker.isArmorCrafted("helm", "corrupted"));
        }

        // Weak monsters needed for shards
        if (WEAK_MONSTER_NAMES.contains(name)) {
            return resourceTracker.getCrystallineShards() < resourceTracker.getPhase1RequiredShards() ||
                    resourceTracker.getCorruptedShards() < resourceTracker.getPhase1RequiredShards();
        }

        // Boss needed if all crafting phases are complete
        return BOSS_NAMES.contains(name) && resourceTracker.isReadyToCraft() && resourceTracker.isPhaseTwoCraftingComplete();
    }

    // Check if the target NPC is a demi-boss
    public static boolean isDemiBoss(Npc target) {
        return target != null && DEMI_BOSS_NAMES.contains(target.getName());
    }

    // Determine if prayer flicking is needed for weak monsters
    public static boolean shouldFlickForWeakMonster(Npc target) {
        return target != null && WEAK_MONSTER_NAMES.contains(target.getName());
    }

    public static boolean hasTeleportCrystal() {
        // Check if there's a "Teleport Crystal" in the inventory
        return Inventory.contains("Teleport Crystal");
    }

    public static void teleportBackToSpawn() {
        // Attempt to use the "Teleport Crystal" if it is present in the inventory
        if (hasTeleportCrystal()) {
            var teleportCrystal = Inventory.getItems("Teleport Crystal").first();
            if (teleportCrystal != null && teleportCrystal.interact("Teleport")) {
                log.info("Teleporting back to spawn using Teleport Crystal.");
                Execution.delayUntil(() -> !Objects.requireNonNull(Players.getLocal()).isMoving(), 2000, 5000); // Wait until teleport completes
            } else {
                log.warn("Failed to interact with the Teleport Crystal.");
            }
        } else {
            log.warn("No Teleport Crystal found in inventory.");
        }
    }

    public static GameObject getNeededResourceNode() {
        // Check resource nodes in the dungeon based on what is still needed
        if (!getResourceTracker().isResourceFullyGathered("Crystal Ore")) {
            return GameObjects.newQuery().names("Crystal Ore").results().nearest();
        } else if (!getResourceTracker().isResourceFullyGathered("Corrupted Ore")) {
            return GameObjects.newQuery().names("Corrupted Ore").results().nearest();
        } else if (!getResourceTracker().isResourceFullyGathered("Phren Bark")) {
            return GameObjects.newQuery().names("Phren Bark").results().nearest();
        } else if (!getResourceTracker().isResourceFullyGathered("Linum Tirinium")) {
            return GameObjects.newQuery().names("Linum Tirinium").results().nearest();
        }

        // Add more resources if necessary
        log.info("All needed resources have been gathered or no nodes are available.");
        return null; // Return null if all resources are gathered or no node is found
    }

    public static boolean newAreaIsAccessible(Coordinate roomLocation) {
        // A HashMap to track scanned rooms, with room coordinates as keys and their state as values
        HashMap<Coordinate, Boolean> scannedRooms = new HashMap<>();

        // Check if this room has already been scanned and marked accessible
        if (scannedRooms.containsKey(roomLocation) && scannedRooms.get(roomLocation)) {
            log.info("Adding roomLocation to scannedRooms: " + roomLocation);
            return true; // Skip if already marked as accessible
        }

        // Check for the "Illuminated Symbol" object to confirm new area activation
        GameObject illuminatedSymbol = GameObjects.newQuery().names("Illuminated Symbol").results().first();

        if (illuminatedSymbol != null) {
            // Mark the room as accessible in the HashMap
            scannedRooms.put(roomLocation, true);
            return true;
        } else {
            return false;
        }
    }


    public static Npc getBoss(BossType bossType) {
        // Replace "BossName" with the actual name of the boss NPC.
        return Npcs.newQuery().names("Crystalline Hunllef", "Corrupted Hunllef").results().first();
    }

    // Crafting area coordinates for Crystalline variant
    public static final Coordinate CRYSTALLINE_CRAFTING_NW = new Coordinate(1906, 5677, 1);
    public static final Coordinate CRYSTALLINE_CRAFTING_SE = new Coordinate(1917, 5666, 1);

    // Crafting area coordinates for Corrupted variant
    public static final Coordinate CORRUPTED_CRAFTING_NW = new Coordinate(1970, 5677, 1);
    public static final Coordinate CORRUPTED_CRAFTING_SE = new Coordinate(1981, 5666, 1);

    public static boolean isInCraftingArea(BossType bossType) {
        Coordinate playerPosition = Objects.requireNonNull(Players.getLocal()).getPosition();

        if (bossType == BossType.CRYSTALLINE_HUNLLEF) {
            assert playerPosition != null;
            return isWithinArea(playerPosition, CRYSTALLINE_CRAFTING_NW, CRYSTALLINE_CRAFTING_SE);
        } else if (bossType == BossType.CORRUPTED_HUNLLEF) {
            assert playerPosition != null;
            return isWithinArea(playerPosition, CORRUPTED_CRAFTING_NW, CORRUPTED_CRAFTING_SE);
        }
        return false;
    }


    public static boolean isWithinArea(Coordinate position, Coordinate northwest, Coordinate southeast) {
        return position.getX() >= northwest.getX() && position.getX() <= southeast.getX()
                && position.getY() >= southeast.getY() && position.getY() <= northwest.getY()
                && position.getPlane() == northwest.getPlane();
    }

    public static boolean isInventoryFull() {
        int maxInventorySize = 28;
        return Inventory.getQuantity() >= maxInventorySize;
    }
}


