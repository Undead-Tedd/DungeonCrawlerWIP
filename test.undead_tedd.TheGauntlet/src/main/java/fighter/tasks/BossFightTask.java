package fighter.tasks;

import com.runemate.game.api.osrs.local.hud.interfaces.Prayer;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.local.hud.interfaces.Health;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.osrs.local.hud.interfaces.ControlPanelTab;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import lombok.extern.log4j.Log4j2;

import java.awt.event.KeyEvent;

@Log4j2(topic = "BossFightTask")
public class BossFightTask extends LeafTask {

    private DungeonCrawler bot;
    private int attackCount = 0; // Track the number of attacks made by the player
    private int bossAttackCycle = 0; // Track the Hunllef's attack cycle (4 attacks per style)
    private boolean isMagicPhase = false; // Toggle between Magic and Ranged phases
    private boolean prayerDisabled = false;

    public BossFightTask(DungeonCrawler bot) {
        this.bot = bot;
    }

    @Override
    public void execute() {
        Npc boss = DungeonUtils.getBoss();

        if (boss == null) {
            log.warn("Unable to find the boss, rechecking...");
            bot.updateTaskStatus("Unable to find the boss, rechecking...");
            return;
        }

        // Prayer and Health management
        managePrayer();
        manageHealth();

        // Ensure inventory is open for gear switching
        ensureInventoryIsOpenUsingHotkey();

        // Handle Hunllef's attack cycle (Magic vs Ranged)
        handleAttackCycle(boss);

        // Handle player attacks and prayer switching
        handlePlayerAttacks(boss);

        // Handle Tornado and tile avoidance
        avoidTornadoesAndTiles();

        // Engage the boss if ready
        if (boss.interact("Attack")) {
            log.info("Engaging the Crystalline Hunllef boss fight!");
            bot.updateTaskStatus("Engaging the Crystalline Hunllef...");
            Execution.delayUntil(() -> Health.getCurrentPercent() < 50, 5000);
            bot.updateTaskStatus("Boss fight in progress, health is below 50%...");
        }
    }

    private void ensureInventoryIsOpenUsingHotkey() {
        if (!ControlPanelTab.INVENTORY.isOpen()) {
            log.info("Opening Inventory tab using hotkey F3...");
            Keyboard.pressKey(KeyEvent.VK_F3);
            Execution.delayUntil(ControlPanelTab.INVENTORY::isOpen, 1200, 1800);
        }
    }

    private void ensurePrayerTabIsOpenUsingHotkey() {
        if (!ControlPanelTab.PRAYER.isOpen()) {
            log.info("Opening Prayer tab using hotkey F5...");
            Keyboard.pressKey(KeyEvent.VK_F5);
            Execution.delayUntil(ControlPanelTab.PRAYER::isOpen, 1200, 1800);
        }
    }

    private void managePrayer() {
        ensurePrayerTabIsOpenUsingHotkey();

        // Restore prayer points with Egniol potions if below 20%
        if (Prayer.getPoints() < 20 && Inventory.contains("Egniol potion")) {
            log.info("Restoring prayer points with Egniol potion.");
            Inventory.getItems("Egniol potion").first().interact("Drink");
        }

        // If prayer gets disabled, re-enable it
        if (prayerDisabled && !Prayer.PROTECT_FROM_MISSILES.isActivated() && !Prayer.PROTECT_FROM_MAGIC.isActivated()) {
            log.info("Re-enabling prayers after being disabled.");
            if (isMagicPhase) {
                Prayer.PROTECT_FROM_MAGIC.activate();
            } else {
                Prayer.PROTECT_FROM_MISSILES.activate();
            }
            prayerDisabled = false;
        }
    }

    private void manageHealth() {
        ensureInventoryIsOpenUsingHotkey();

        // Restore health with Egniol potions if below 20%
        if (Health.getCurrentPercent() < 20 && Inventory.contains("Egniol potion")) {
            log.info("Restoring health with Egniol potion.");
            Inventory.getItems("Egniol potion").first().interact("Drink");
        }
    }

    private void handleAttackCycle(Npc boss) {
        // Hunllef's attack cycle: 4 Ranged -> 4 Magic
        if (bossAttackCycle == 4) {
            isMagicPhase = !isMagicPhase; // Toggle between Magic and Ranged phases
            bossAttackCycle = 0;
        }

        // Switch prayers accordingly
        if (isMagicPhase) {
            Prayer.PROTECT_FROM_MAGIC.activate();
            log.info("Switched to Protect from Magic.");
            bot.updateTaskStatus("Switched to Protect from Magic.");
        } else {
            Prayer.PROTECT_FROM_MISSILES.activate();
            log.info("Switched to Protect from Missiles.");
            bot.updateTaskStatus("Switched to Protect from Missiles.");
        }
        bossAttackCycle++;
    }

    private void handlePlayerAttacks(Npc boss) {
        if (attackCount == 6) {
            switchAttackStyle();
            attackCount = 0; // Reset attack count after switching
        }

        if (boss.interact("Attack")) {
            log.info("Attacking boss, attack count: " + attackCount);
            attackCount++;
        }
    }

    private void switchAttackStyle() {
        log.info("Switching attack style to avoid Hunllef's protection prayer.");
        bot.updateTaskStatus("Switching attack style...");

        if (isMagicPhase) {
            if (Inventory.contains("Magic staff")) {
                Inventory.getItems("Magic staff").first().interact("Wield");
                log.info("Equipped Magic staff.");
                bot.updateTaskStatus("Equipped Magic staff.");
            }
        } else {
            if (Inventory.contains("Bow")) {
                Inventory.getItems("Bow").first().interact("Wield");
                log.info("Equipped Bow.");
                bot.updateTaskStatus("Equipped Bow.");
            }
        }
    }

    private void avoidTornadoesAndTiles() {
        // Logic to avoid tornadoes and dangerous tiles.
        // Can use Movement API to walk away from dangerous areas.
    }
}
