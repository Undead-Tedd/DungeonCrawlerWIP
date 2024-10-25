package fighter.tasks;

import com.runemate.game.api.osrs.local.hud.interfaces.Prayer;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.region.GroundItems;
import com.runemate.game.api.hybrid.entities.GroundItem;
import fighter.DebugConsole;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import fighter.tasks.tracking.ResourceTracker;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "CombatTask")
public class CombatTask extends LeafTask {
    private static final String SCEPTRE = "Crystal Sceptre"; // Default weapon for initial combat
    private static final String[] WEAPONS = {"Crystal Staff", "Crystal Bow"}; // Tier 2 weapons
    private static final String[] LOOT_ITEMS = {"Crystal shards", "Weapon Frame", "Raw paddlefish", "Grym leaf", "Teleport Crystal"};
    private final DungeonCrawler bot;
    private final DebugConsole debugConsole;
    private Npc target;
    private boolean dragonKilled = false; // Tracks if we've killed 1 Crystalline Dragon
    private boolean darkBeastKilled = false; // Tracks if we've killed 1 Crystalline Dark Beast
    private final ResourceTracker resourceTracker;

    public CombatTask(DungeonCrawler bot) {
        this.bot = bot;
        this.debugConsole = bot.getDebugConsole(); // Initialize debug console from the bot
        this.resourceTracker = bot.getResourceTracker();
    }

    @Override
    public void execute() {
        debugConsole.log("Starting combat task...");
        log.info("Starting combat task...");
        // Ensure we equip the best available weapon
        equipBestWeapon();
        // Update UI with combat status
        bot.updateTaskStatus("Looking for necessary monsters to kill...");
        debugConsole.log("Looking for necessary monsters to kill...");
        // Skip combat if both a dragon and dark beast have been killed
        if (dragonKilled && darkBeastKilled) {
            bot.updateTaskStatus("Both dragon and dark beast killed, focusing on gathering and crafting...");
            debugConsole.log("Both dragon and dark beast killed, focusing on gathering and crafting...");
            return;  // Exit combat task and focus on gathering or crafting
        }
        // Look for a weak monster to attack
        log.info("Looking for necessary weak monsters to attack.");
        debugConsole.log("Looking for necessary weak monsters to attack.");
        target = DungeonUtils.getWeakMonster();
        if (target == null) {
            log.info("No weak monsters found, checking for demi-bosses.");
            debugConsole.log("No weak monsters found, checking for demi-bosses.");
            target = DungeonUtils.getDemiBoss();  // Check for demi-boss if no weak monsters are found
        }
        // Skip boss fights until the appropriate time
        if (target != null && DungeonUtils.isBossOrDemiBoss(target)) {
            log.info("Avoiding boss or demi-boss interaction for now.");
            debugConsole.log("Avoiding boss or demi-boss interaction for now.");
            bot.updateTaskStatus("Skipping boss/demi-boss for now.");
            return;
        }
        // Ensure we only attack necessary monsters and skip unnecessary ones
        if (target != null && DungeonUtils.isNeededMonster(target, resourceTracker) && target.interact("Attack")) {
            log.info("Attacking necessary monster: " + target.getName());
            debugConsole.log("Attacking necessary monster: " + target.getName());
            bot.updateTaskStatus("Attacking " + target.getName() + "...");
            // Track when we kill the Crystalline Dragon or Crystalline Dark Beast
            if (target.getName().contains("Dragon")) {
                dragonKilled = true;
                log.info("Killed Crystalline Dragon. Dragon kill limit reached.");
                debugConsole.log("Killed Crystalline Dragon. Dragon kill limit reached.");
            } else if (target.getName().contains("Dark Beast")) {
                darkBeastKilled = true;
                log.info("Killed Crystalline Dark Beast. Dark Beast kill limit reached.");
                debugConsole.log("Killed Crystalline Dark Beast. Dark Beast kill limit reached.");
            }
            // Prayer flick logic based on the target type
            if (DungeonUtils.isDemiBoss(target)) {
                log.info("Flicking prayer for demi-boss: " + target.getName());
                debugConsole.log("Flicking prayer for demi-boss: " + target.getName());
                DungeonUtils.flickPrayer(Prayer.PROTECT_FROM_MELEE);
            } else if (DungeonUtils.shouldFlickForWeakMonster(target)) {
                log.info("Flicking prayer for weak monster: " + target.getName());
                debugConsole.log("Flicking prayer for weak monster: " + target.getName());
                DungeonUtils.flickPrayer(Prayer.PROTECT_FROM_MELEE);  // Flick prayer for the weak monster
            }
            // Collect loot after combat
            collectLoot();
            bot.updateTaskStatus("Looting items...");
            debugConsole.log("Looting items...");
        } else {
            log.warn("No necessary monsters found to attack.");
            debugConsole.log("No necessary monsters found to attack.");
            bot.updateTaskStatus("No necessary monsters found.");
        }
    }

    // Equip the best available weapon, prioritizing Tier 2 weapons (Crystal Staff or Crystal Bow)
    private void equipBestWeapon() {
        String equippedWeapon = Inventory.getSelectedItem() != null ? Inventory.getSelectedItem().getDefinition().getName() : null;
        // Prioritize equipping the first Tier 2 weapon found (Staff or Bow)
        for (String weapon : WEAPONS) {
            if (Inventory.contains(weapon) && !weapon.equals(equippedWeapon)) {
                log.info("Equipping best weapon: " + weapon);
                debugConsole.log("Equipping best weapon: " + weapon);
                bot.updateTaskStatus("Equipping " + weapon + "...");
                Inventory.getItems(weapon).first().interact("Wield");
                Execution.delay(600, 1000);
                return; // Exit once the Tier 2 weapon is equipped
            }
        }
        // Equip Crystal Sceptre only if no Tier 2 weapon is equipped or available
        if (Inventory.contains(SCEPTRE) && !SCEPTRE.equals(equippedWeapon) && !hasBetterWeaponEquipped()) {
            log.info("Equipping default weapon: Crystal Sceptre.");
            debugConsole.log("Equipping default weapon: Crystal Sceptre.");
            bot.updateTaskStatus("Equipping Crystal Sceptre...");
            Inventory.getItems(SCEPTRE).first().interact("Wield");
            Execution.delay(600, 1000);
        }
    }

    // Check if a Tier 2 weapon (Staff or Bow) is already equipped
    private boolean hasBetterWeaponEquipped() {
        String equippedWeapon = Inventory.getSelectedItem() != null ? Inventory.getSelectedItem().getDefinition().getName() : null;
        for (String weapon : WEAPONS) {
            if (weapon.equals(equippedWeapon)) {
                return true; // A Tier 2 weapon is already equipped
            }
        }
        return false; // No Tier 2 weapon is equipped
    }

    // Collect lootable items from the ground after defeating monsters
    private void collectLoot() {
        // First, check if space is available for looting
        if (DungeonUtils.isLootNeeded()) {
            // Find and collect lootable items from the ground
            GroundItems.newQuery().names(LOOT_ITEMS).results().forEach(item -> {
                if (DungeonUtils.isNeededResource(item.getDefinition().getName())) {
                    log.info("Looting item: " + item.getDefinition().getName());
                    debugConsole.log("Looting item: " + item.getDefinition().getName());
                    bot.updateTaskStatus("Looting " + item.getDefinition().getName() + "...");
                    item.interact("Take");
                    Execution.delay(500, 1000); // Delay based on looting speed
                } else {
                    log.info("Skipping unnecessary loot: " + item.getDefinition().getName());
                    debugConsole.log("Skipping unnecessary loot: " + item.getDefinition().getName());
                }
            });
        } else {
            log.warn("No space for looting. Awaiting inventory management.");
            debugConsole.log("No space for looting. Awaiting inventory management.");
            bot.updateTaskStatus("No space for looting. Waiting for paddlefish to be dropped.");
        }
    }
}
