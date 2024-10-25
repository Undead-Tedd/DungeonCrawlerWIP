package fighter;

import java.util.List;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.GroundItem;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.local.Camera;
import com.runemate.game.api.hybrid.local.hud.interfaces.*;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.GroundItems;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.osrs.local.hud.interfaces.Prayer;
import fighter.tasks.tracking.ResourceTracker;
import com.runemate.game.api.hybrid.local.hud.interfaces.SpriteItem;
import lombok.extern.log4j.Log4j2;

import java.awt.event.KeyEvent;

import static fighter.tasks.tracking.ResourceTracker.isNeededResource;

@Log4j2
public class DungeonUtils {

    private static final long FLICK_DELAY = 600; // Flick every 600ms
    private static boolean secondReturnToBase = false; // Tracks second base return for teleport crystal

    // Check for starting area (Bryn and reward chest)
    public static boolean isInStartingRoom() {
        Npc bryn = Npcs.newQuery().names("Bryn").results().first();
        GameObject rewardChest = GameObjects.newQuery().names("Reward Chest").results().first();
        return bryn != null && rewardChest != null;
    }

    public static GameObject getGauntletEntrancePlatform() {
        return GameObjects.newQuery().names("The Gauntlet").results().first();
    }

    public static boolean enterGauntlet(boolean enterCorrupted) {
        GameObject platform = getGauntletEntrancePlatform();
        if (platform != null) {
            log.info("Starting our Gauntlet Run, GL!");
            if (enterCorrupted) {
                return platform.interact("Enter-corrupted");
            } else {
                return platform.interact("Enter");
            }
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

    // Method to restore health
    public static void restoreHealth() {
        if (Inventory.contains("Paddlefish")) {
            var paddlefish = Inventory.getItems("Paddlefish").first();
            if (paddlefish != null) {
                paddlefish.interact("Eat");
                log.info("Eating paddlefish to restore HP.");
                Execution.delay(500, 1000); // Allow some time for the action
            } else {
                log.warn("No paddlefish found in inventory.");
            }
        } else {
            log.warn("We are out of food, good luck us!");
        }
    }

    // Method to restore prayer points using Egniol Potion
    public static void restorePrayer() {
        var egniolPotion = Inventory.getItems(item -> item.getDefinition() != null && item.getDefinition().getName().contains("Egniol Potion")).first();
        if (egniolPotion != null) {
            egniolPotion.interact("Drink");
            log.info("Drinking Egniol Potion to restore prayer/run energy.");
            Execution.delay(500, 1000); // Allow some time for the action
        } else {
            log.warn("We are out of Egniol Potions, good luck us!");
        }
    }

    // Method for retrieving resource nodes
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
            flickPrayer(Prayer.PROTECT_FROM_MELEE);  // You can change the prayer based on the situation
        } else {
            if (shouldHeal()) {
                log.info("Health low, need to heal.");
                healPlayer();
            } else if (Players.getLocal().isMoving()) {
                log.info("Player is moving, keeping prayer active.");
                keepPrayerActive(Prayer.PROTECT_FROM_MELEE);
            }
        }
    }

    // Decide when to flick prayer
    public static boolean shouldPrayerFlick() {
        return Prayer.getPoints() < 20 && !Players.getLocal().isMoving() && !Inventory.contains("Egniol Potion");
    }

    public static void flickPrayer(Prayer prayer) {
        switchToPrayerInterface();  // Ensure we are in the prayer interface

        while (shouldPrayerFlick()) {
            if (!prayer.isActivated()) {
                prayer.activate();
                log.info("Prayer activated: " + prayer);
            }
            Execution.delay(FLICK_DELAY);
            prayer.deactivate();
            log.info("Prayer deactivated: " + prayer);
            Execution.delay(FLICK_DELAY); // Delay to match game tick cycles
        }
    }

    // Keep prayer active without flicking
    public static void keepPrayerActive(Prayer prayer) {
        if (!prayer.isActivated()) {
            prayer.activate();  // Activate the prayer
            log.info("Activated prayer without flicking: " + prayer);
        }
    }

    // Switch to prayer interface via hotkey
    public static void switchToPrayerInterface() {
        if (!com.runemate.game.api.osrs.local.hud.interfaces.ControlPanelTab.PRAYER.isOpen()) {
            Keyboard.pressKey(KeyEvent.VK_F1); // Example: F1 switches to prayer interface
            log.info("Switched to prayer interface.");
            Execution.delay(300, 500);
        }
    }

    // Switch to inventory interface via hotkey
    public static void switchToInventoryInterface() {
        if (!com.runemate.game.api.osrs.local.hud.interfaces.ControlPanelTab.INVENTORY.isOpen()) {
            Keyboard.pressKey(KeyEvent.VK_F2); // Example: F2 switches to inventory interface
            log.info("Switched to inventory interface.");
            Execution.delay(300, 500);
        }
    }

    // Heal the player by eating food
    public static void healPlayer() {
        switchToInventoryInterface();
        restoreHealth();
        switchToPrayerInterface(); // Return to prayer interface after healing
    }

    // Method to retrieve the Singing Bowl object in the environment
    public static GameObject getSingingBowl() {
        GameObject singingBowl = GameObjects.newQuery().names("Singing Bowl").results().first();
        if (singingBowl != null) {
            log.info("Found the Singing Bowl.");
        } else {
            log.warn("Singing Bowl not found.");
        }
        return singingBowl;
    }

    // Logic for crafting items
    public static boolean craftItems() {
        GameObject singingBowl = getSingingBowl();
        if (singingBowl != null && singingBowl.interact("Craft")) {
            log.info("Opened the Singing Bowl crafting menu.");
            Execution.delay(600);  // Wait for the crafting menu to open
            return true;
        }
        log.warn("Unable to craft items.");
        return false;
    }

    // Method to pass the boss room barrier
    public static boolean passBossRoomBarrier() {
        GameObject barrier = GameObjects.newQuery().names("Barrier").results().first();
        if (barrier != null && barrier.interact("Pass")) {
            log.info("Passing the boss room barrier.");
            Execution.delay(2000);  // Delay to allow confirmation dialog to appear
            InterfaceComponent confirmButton = Interfaces.newQuery().texts("Yes, I'm ready.").results().first();
            if (confirmButton != null && confirmButton.isVisible() && confirmButton.interact("Continue")) {
                log.info("Confirmed boss room entry, entering the fight!");
                return true;
            } else {
                log.warn("Could not confirm boss room entry.");
            }
        }
        log.warn("Boss room barrier not found.");
        return false;
    }

    // Method to retrieve a weak monster for combat
    public static Npc getWeakMonster() {
        return Npcs.newQuery().names("Spider", "Rat", "Bat").results().first();
    }

    // Method to retrieve a demi-boss for combat
    public static Npc getDemiBoss() {
        return Npcs.newQuery().names("Dark Beast", "Dragon", "Bear").results().first();
    }

    // Helper method to check if a monster is a demi-boss
    public static boolean isDemiBoss(Npc target) {
        String npcName = target.getName();
        return npcName.equals("Dark Beast") || npcName.equals("Dragon") || npcName.equals("Bear");
    }

    // Method to retrieve the boss for the boss fight
    public static Npc getBoss() {
        return Npcs.newQuery().names("Hunllef", "Corrupted Hunllef").results().first();
    }

    // Check if player has a Teleport Crystal
    public static boolean hasTeleportCrystal() {
        return Inventory.contains("Teleport Crystal");
    }

    // Check if the player has completed certain actions or gathered resources
    public static boolean hasResourceItem(String... resourceNames) {
        for (String resourceName : resourceNames) {
            if (Inventory.contains(resourceName)) {
                return true;
            }
        }
        return false;
    }

    public static GameObject getLightNodeAtDoor() {
        log.info("Finding light node at the door...");
        GameObject lightNode = GameObjects.newQuery().names("Node").results().first();
        if (lightNode != null) {
            log.info("Found light node at the door.");
            return lightNode;
        } else {
            log.warn("Light node not found at the door.");
            return null;
        }
    }

    public static boolean shouldFlickForWeakMonster(Npc target) {
        // Logic to decide if prayer flicking is needed for weak monsters
        return Health.getCurrentPercent() < 70 && Players.getLocal().distanceTo(target) <= 3 && !Inventory.contains("Egniol Potion");
    }

    // Add this for C-shaped path logic
    public static void followCShapedPath(ResourceTracker tracker) {
        log.info("Following C-shaped path around the start...");

        for (String room : new String[]{"Room 1", "Room 2", "Room 3"}) {
            if (enterRoom(room)) {
                log.info("Entered room: " + room);

                // Check for weak monsters and attack
                Npc monster = getWeakMonster();
                if (monster != null && monster.interact("Attack")) {
                    log.info("Attacking weak monster: " + monster.getName());
                    Execution.delayUntil(() -> !monster.isValid(), 5000, 10000);
                }

                // Gather resources
                for (String resource : new String[]{"Crystal Ore", "Phren Bark", "Linum Tirinium"}) {
                    GameObject resourceNode = GameObjects.newQuery().names(resource).results().first();
                    if (resourceNode != null && resourceNode.interact("Gather")) {
                        log.info("Gathering resource: " + resourceNode.getDefinition().getName());
                        tracker.trackCrystallineShards(1);
                        Execution.delay(600, 1000);
                    }
                }

                exitRoom(room);
            } else {
                log.warn("Could not enter room: " + room);
            }
        }

        log.info("Completed the C-shaped path.");
    }

    // Example methods to enter and exit rooms
    public static boolean enterRoom(String roomName) {
        log.info("Attempting to enter room: " + roomName);
        GameObject door = GameObjects.newQuery().names("Door to " + roomName).results().first();
        if (door != null && door.interact("Enter")) {
            Execution.delayUntil(() -> Players.getLocal().distanceTo(door) > 5, 5000);
            return true;
        }
        return false;
    }

    public static void exitRoom(String roomName) {
        log.info("Exiting room: " + roomName);
        GameObject exit = GameObjects.newQuery().names("Exit from " + roomName).results().first();
        if (exit != null && exit.interact("Exit")) {
            Execution.delayUntil(() -> Players.getLocal().distanceTo(exit) > 5, 5000);
        }
    }

    public static String getPlayerOrientation() {
        int yaw = Camera.getYaw();
        // Determine direction based on the yaw angle (approximate values)
        if (yaw >= 315 || yaw < 45) {
            return "north";
        } else if (yaw >= 45 && yaw < 135) {
            return "east";
        } else if (yaw >= 135 && yaw < 225) {
            return "south";
        } else {
            return "west";
        }
    }

    public static boolean checkBossPosition() {
        log.info("Checking boss position by adjusting camera yaw...");
        return !getBossDirection().equals("unknown");
    }

    public static String getBossDirection() {
        // Adjust the camera to face different directions and check for the boss or boss barrier
        String[] directions = {"north", "east", "south", "west"};
        for (String direction : directions) {
            // Adjust camera yaw based on direction
            switch (direction) {
                case "north":
                    Camera.concurrentlyTurnTo(0);
                    break;
                case "east":
                    Camera.concurrentlyTurnTo(90);
                    break;
                case "south":
                    Camera.concurrentlyTurnTo(180);
                    break;
                case "west":
                    Camera.concurrentlyTurnTo(270);
                    break;
            }

            // Check for the boss or boss barrier
            Execution.delay(1000); // Wait for the camera to adjust
            if (Npcs.newQuery().names("Crystalline Hunllef", "Corrupted Hunllef").results().first() != null) {
                log.info("Boss found in the " + direction + " direction.");
                return direction;
            }
            if (GameObjects.newQuery().names("Barrier").results().first() != null) {
                log.info("Boss barrier found in the " + direction + " direction.");
                return direction;
            }
        }

        // If no boss or barrier is found, return unknown
        log.warn("Unable to determine the boss direction.");
        return "unknown";
    }


    // Adjusted C-shaped path logic based on player's orientation and boss location
    public static boolean followCShapedPathAroundStart() {
        String bossDirection = getBossDirection();

        log.info("Boss direction determined: " + bossDirection);

        // Adjust C-shaped path according to the determined boss location
        switch (bossDirection) {
            case "north":
                log.info("Navigating the C-shaped path, starting in the south direction.");
                break;
            case "south":
                log.info("Navigating the C-shaped path, starting in the north direction.");
                break;
            case "east":
                log.info("Navigating the C-shaped path, starting in the west direction.");
                break;
            case "west":
                log.info("Navigating the C-shaped path, starting in the east direction.");
                break;
            default:
                log.warn("Unable to determine the boss direction.");
                return false; // Unable to determine path
        }

        // Execute the C-shaped path navigation
        return true; // Return success after navigation
    }

    // Method to check if the NPC is the main boss (Crystalline Hunllef or Corrupted Hunllef)
    public static boolean isBoss(Npc target) {
        if (target == null) {
            return false;
        }

        String npcName = target.getName();
        return npcName.equals("Crystalline Hunllef") || npcName.equals("Corrupted Hunllef");
    }

    // Method to check if the NPC is either a boss or a demi-boss
    public static boolean isBossOrDemiBoss(Npc target) {
        return isBoss(target) || isDemiBoss(target);
    }

    // Loot and resource management
    public static boolean isLootNeeded() {
        GroundItem loot = GroundItems.newQuery().filter(item -> isNeededResource(item.getDefinition().getName())).results().first();

        if (loot == null) {
            log.info("No needed loot found.");
            return false;
        }

        String lootName = loot.getDefinition().getName();
        log.info("Needed loot found: " + lootName);

        // If the loot is high-priority and the inventory is full, drop paddlefish to make space
        if (Inventory.isFull()) {
            log.info("Inventory is full. Checking if paddlefish should be dropped for high-priority loot.");

            if (isHighPriorityLoot(lootName)) {
                log.info("High-priority loot detected: " + lootName);
                if (shouldDropPaddlefishForLoot()) {
                    dropPaddlefish();
                    return true; // Indicate we need to pick up the loot after dropping paddlefish
                }
            } else {
                log.info("Loot found, but no space and not high-priority: " + lootName);
                return false;
            }
        }

        // If the inventory isn't full, we can safely loot
        return true;
    }

    // Check if the loot is a high-priority item
    public static boolean isHighPriorityLoot(String lootName) {
        return lootName.equals("Weapon Frame") || lootName.equals("Corrupted Orb") || lootName.equals("Crystalline Orb")
                || lootName.equals("Corrupted Bowstring") || lootName.equals("Crystalline Bowstring");
    }

    // Check if we should drop paddlefish for high-priority loot
    public static boolean shouldDropPaddlefishForLoot() {
        return Inventory.contains("Raw Paddlefish") && isInventoryFull();
    }

    private static boolean isInventoryFull() {
        return Inventory.isFull();
    }


    private static ResourceTracker resourceTracker; // Declaring resourceTracker

    // Method to initialize or get the existing ResourceTracker instance
    public static ResourceTracker getResourceTracker() {
        if (resourceTracker == null) {
            resourceTracker = new ResourceTracker(); // Initialize if not already done
            log.info("Initialized a new ResourceTracker.");
        }
        return resourceTracker;
    }

    // Drop paddlefish to make space for high-priority loot
    public static void dropPaddlefish() {
        List<SpriteItem> paddlefish = Inventory.getItems("Raw Paddlefish").asList(); // Update to SpriteItem

        if (!paddlefish.isEmpty()) {
            log.info("Dropping raw paddlefish to make space for high-priority loot.");

            // Drop one paddlefish at a time until there is enough space for loot
            for (SpriteItem fish : paddlefish) { // Use SpriteItem here
                if (!Inventory.isFull()) {
                    log.info("Inventory space made. Ready to pick up loot.");
                    break;
                }
                fish.interact("Drop"); // interact is available for SpriteItem
                Execution.delay(300, 600); // Delay to simulate dropping speed
            }
        } else {
            log.warn("No raw paddlefish in inventory to drop.");
        }
    }

    // Checking needed resources with differentiation for corrupted and crystalline

    public static int getShardCount(String type) {
        if (type.equals("corrupted")) {
            return getResourceTracker().getCorruptedShards();
        } else if (type.equals("crystalline")) {
            return getResourceTracker().getCrystallineShards();
        }
        return 0;
    }

    public static int getOrbCount(String type) {
        if (type.equals("corrupted")) {
            return getResourceTracker().getCorruptedOrbCount();
        } else if (type.equals("crystalline")) {
            return getResourceTracker().getCrystallineOrbCount();
        }
        return 0;
    }

    public static GameObject getNeededResourceNode() {
        // Query resource nodes based on their names
        return GameObjects.newQuery().names(
                "Crystal Deposit", "Linum Tirinum", "Phren Roots", "Grym Root", "Fishing Spot"
        ).results().first();
    }

    public class DungeonUtils {
        public static final int REQUIRED_ORE = 3;
        public static final int REQUIRED_BARK = 3;
        public static final int REQUIRED_TIRINIUM = 3;
        public static final int REQUIRED_SHARDS = 380;
        public static final int REQUIRED_ORB = 1;
        public static final int REQUIRED_BOWSTRING = 1;
        public static final int REQUIRED_WEAPON_FRAME = 2;
        public static final int REQUIRED_DUST = 30;

        public static ResourceTracker getResourceTracker() {
            // Logic to get the resource tracker
        }
    }

    // Method to check if a teleport crystal needs to be crafted (Unchanged)
    public static boolean isTeleportCrystalNeeded() {
        if (!secondReturnToBase && !Inventory.contains("Teleport Crystal")) {
            log.info("No teleport crystal found, it needs to be crafted.");
            return true;
        }
        log.info("Teleport crystal is not needed anymore.");
        return false;
    }

    public static void markSecondReturnToBase() {
        secondReturnToBase = true;
    }

    // Crafting logic for Bows and Staffs

    // Check if a bow needs to be crafted (based on the current tier)
    public static boolean isBowNeeded(String tier) {
        return !getResourceTracker().isBowCrafted(tier);
    }

    // Check if a staff needs to be crafted (based on the current tier)
    public static boolean isStaffNeeded(String tier) {
        return !getResourceTracker().isStaffCrafted(tier);
    }

    // Mark the crafting of a bow tier
    public static void markBowCrafted(String tier) {
        getResourceTracker().trackBowCrafting(tier);
    }

    // Mark the crafting of a staff tier
    public static void markStaffCrafted(String tier) {
        getResourceTracker().trackStaffCrafting(tier);
    }

    // Example logic: Decide if we should craft the next bow tier
    public static void handleBowCrafting(String currentTier) {
        if (isBowNeeded(currentTier)) {
            log.info(currentTier + " Bow needs to be crafted.");
            // Add logic to handle crafting here, e.g., using the Singing Bowl
        } else {
            log.info(currentTier + " Bow is already crafted.");
        }
    }

    // Example logic: Decide if we should craft the next staff tier
    public static void handleStaffCrafting(String currentTier) {
        if (isStaffNeeded(currentTier)) {
            log.info(currentTier + " Staff needs to be crafted.");
            // Add logic to handle crafting here, e.g., using the Singing Bowl
        } else {
            log.info(currentTier + " Staff is already crafted.");
        }
    }

    public static boolean allRequiredMonstersSlain() {
        // Define necessary monster types
        List<String> weakMonsters = List.of("Spider", "Rat", "Bat");
        List<String> demiBosses = List.of("Crystalline Dark Beast", "Crystalline Dragon", "Crystalline Bear");

        // Check if all weak monsters and demi-bosses are slain
        for (String monster : weakMonsters) {
            if (Npcs.newQuery().names(monster).results().first() != null) {
                log.info("Not all required weak monsters have been slain. Missing: " + monster);
                return false;
            }
        }
        for (String demiBoss : demiBosses) {
            if (Npcs.newQuery().names(demiBoss).results().first() != null) {
                log.info("Not all required demi-bosses have been slain. Missing: " + demiBoss);
                return false;
            }
        }
        log.info("All required monsters have been slain.");
        return true;
    }

    public static boolean allRequiredMonstersSlain(ResourceTracker tracker) {
        // Check if all required weak monsters have been slain
        List<String> weakMonsters = List.of("Spider", "Rat", "Bat");
        for (String monster : weakMonsters) {
            if (Npcs.newQuery().names(monster).results().first() != null) {
                log.info("Not all required weak monsters have been slain. Missing: " + monster);
                return false;
            }
        }

        // Check if all required demi-bosses have been slain
        List<String> demiBosses = List.of("Crystalline Dark Beast", "Crystalline Dragon", "Crystalline Bear");
        for (String demiBoss : demiBosses) {
            if (Npcs.newQuery().names(demiBoss).results().first() != null) {
                log.info("Not all required demi-bosses have been slain. Missing: " + demiBoss);
                return false;
            }
        }

        log.info("All required monsters have been slain.");
        return true;
    }

    public static boolean isNeededMonster(Npc monster, ResourceTracker tracker) {
        String name = monster.getName();

        // Check if the monster is needed based on ResourceTracker criteria
        if (name.equals("Crystalline Dark Beast") && tracker.getCrystallineBowstringCount() < 1) {
            log.info("Needed monster found: " + name);
            return true;
        } else if (name.equals("Crystalline Dragon") && tracker.getCrystallineOrbCount() < 1) {
            log.info("Needed monster found: " + name);
            return true;
        } else if (name.equals("Crystalline Bear") && tracker.getCrystallineShards() < 380) {
            log.info("Needed monster found: " + name);
            return true;
        }

        log.info("Monster is not needed: " + name);
        return false;
    }

    public static boolean cookAtRange() {
        // Find a cooking range in the game
        GameObject cookingRange = GameObjects.newQuery().names("Range").results().first();
        if (cookingRange != null) {
            log.info("Found cooking range.");
            // Interact with the range to cook food
            if (cookingRange.interact("Cook")) {
                log.info("Started cooking at the range.");
                Execution.delay(2000, 3000); // Wait for the cooking interaction
                return true;
            } else {
                log.warn("Failed to interact with the cooking range.");
            }
        } else {
            log.warn("Cooking range not found.");
        }
        return false;
    }

    public static boolean isNeededLoot(@NonNull String name) {
    }

    public static void teleportBackToSpawn() {
    }
}

