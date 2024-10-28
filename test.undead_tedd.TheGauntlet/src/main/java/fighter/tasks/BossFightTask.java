package fighter.tasks;

import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.status.Hitsplat;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.local.hud.interfaces.Health;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.location.navigation.cognizant.ScenePath;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.region.SpotAnimations;
import com.runemate.game.api.osrs.local.hud.interfaces.ControlPanelTab;
import com.runemate.game.api.osrs.local.hud.interfaces.Prayer;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.tree.LeafTask;
import fighter.DungeonCrawler;
import fighter.DungeonUtils;
import lombok.extern.log4j.Log4j2;

import java.awt.event.KeyEvent;
import java.util.*;

@Log4j2(topic = "BossFightTask")
public class BossFightTask extends LeafTask {

    private final DungeonCrawler bot;
    private final BossType bossType;
    private int bossAttackCycle = 0;
    private boolean isMagicPhase = false;
    private boolean prayerDisabled = false;
    private final Map<Coordinate, Integer> initialTileColors = new HashMap<>();
    private List<Hitsplat> playerHitsplats = new ArrayList<>();
    private List<Hitsplat> bossHitsplats = new ArrayList<>();

    public BossFightTask(DungeonCrawler bot, BossType bossType) {
        this.bot = bot;
        this.bossType = bossType;
        scanInitialSafeTiles();
    }

    @Override
    public void execute() {
        Npc boss = DungeonUtils.getBoss(bossType); // Fetch boss based on variant

        if (boss == null) {
            log.error("Unable to find the boss after multiple attempts.");
            bot.updateTaskStatus("Unable to find the boss, stopping task...");
            return;
        }

        managePrayer();
        manageHealth();
        ensureInventoryIsOpenUsingHotkey();
        handleAttackCycle(boss);

        monitorHitsplats(boss);
        avoidHazardsAndAttackBoss(boss);
    }

    private void ensureInventoryIsOpenUsingHotkey() {
        if (!ControlPanelTab.INVENTORY.isOpen()) {
            log.info("Opening Inventory tab using hotkey F3...");
            Keyboard.pressKey(KeyEvent.VK_F3);
            Execution.delayUntil(ControlPanelTab.INVENTORY::isOpen, 1200, 1800);
        }
    }

    private void managePrayer() {
        if (!ControlPanelTab.PRAYER.isOpen()) {
            log.info("Opening Prayer tab using hotkey F5...");
            Keyboard.pressKey(KeyEvent.VK_F5);
            Execution.delayUntil(ControlPanelTab.PRAYER::isOpen, 1200, 1800);
        }

        if (Prayer.getPoints() < 20 && Inventory.contains("Egniol potion")) {
            log.info("Restoring prayer points with Egniol potion.");
            Objects.requireNonNull(Inventory.getItems("Egniol potion").first()).interact("Drink");
        }

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
        if (Health.getCurrentPercent() < 20 && Inventory.contains("Egniol potion")) {
            log.info("Restoring health with Egniol potion.");
            Objects.requireNonNull(Inventory.getItems("Egniol potion").first()).interact("Drink");
        }
    }

    private void handleAttackCycle(Npc boss) {
        if (bossAttackCycle == 4) {
            isMagicPhase = !isMagicPhase;
            bossAttackCycle = 0;
        }
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

    private void monitorHitsplats(Npc boss) {
        List<Hitsplat> currentPlayerHitsplats = Objects.requireNonNull(Players.getLocal()).getHitsplats();
        List<Hitsplat> currentBossHitsplats = boss.getHitsplats();

        if (hasNewHitsplat(currentPlayerHitsplats, playerHitsplats)) {
            log.info("Player took damage! Re-evaluating conditions...");
            avoidHazardsAndAttackBoss(boss);
        }

        if (hasNewHitsplat(currentBossHitsplats, bossHitsplats)) {
            log.info("Boss took damage from our attack.");
            attackBossIfInRange(boss);
        }

        playerHitsplats = currentPlayerHitsplats;
        bossHitsplats = currentBossHitsplats;
    }

    private boolean hasNewHitsplat(List<Hitsplat> currentHitsplats, List<Hitsplat> previousHitsplats) {
        for (Hitsplat hitsplat : currentHitsplats) {
            if (!previousHitsplats.contains(hitsplat)) {
                return true;
            }
        }
        return false;
    }

    private void avoidHazardsAndAttackBoss(Npc boss) {
        Coordinate safeCoordinate = findNearestSafeCoordinate();
        if (safeCoordinate != null && safeCoordinate.distanceTo(boss) > 1) {
            moveToCoordinate(safeCoordinate);
            Execution.delay(100, 300);  // Delay before attacking the boss
            attackBossIfInRange(boss);
        } else {
            log.warn("No safe tile found or tile is too close to boss.");
        }
        Execution.delay(300);  // Maintain a steady loop interval
    }

    private void scanInitialSafeTiles() {
        for (Coordinate coord : getRoomCoordinates()) {
            int initialColor = getTileColor(coord);
            initialTileColors.put(coord, initialColor);
        }
    }

    private Iterable<Coordinate> getRoomCoordinates() {
        return bossType.getRoomCoordinates();  // Get room coordinates based on boss type
    }

    private Coordinate findNearestSafeCoordinate() {
        Coordinate nearestSafeCoordinate = null;
        double nearestDistance = Double.MAX_VALUE;
        Coordinate playerPosition = Objects.requireNonNull(Players.getLocal()).getPosition();

        for (Coordinate coord : getRoomCoordinates()) {
            if (isCoordinateSafe(coord)) {
                assert playerPosition != null;
                double distance = playerPosition.distanceTo(coord);
                if (distance < nearestDistance) {
                    nearestSafeCoordinate = coord;
                    nearestDistance = distance;
                }
            }
        }
        return nearestSafeCoordinate;
    }

    private boolean isCoordinateSafe(Coordinate coord) {
        int initialColor = initialTileColors.getOrDefault(coord, -1);
        int currentColor = getTileColor(coord);
        return initialColor == currentColor && !isHazardousSpotAnimation(coord);
    }

    private int getTileColor(Coordinate coord) {
        // Placeholder - Replace with actual logic to retrieve tile color
        return -1;
    }

    private boolean isHazardousSpotAnimation(Coordinate coord) {
        return SpotAnimations.newQuery()
                .results()
                .stream()
                .anyMatch(animation -> Objects.equals(animation.getPosition(), coord));
    }

    private void moveToCoordinate(Coordinate coord) {
        ScenePath path = ScenePath.buildTo(coord);
        if (path != null) {
            log.info("Moving to safe coordinate: " + coord);
            path.step();
        }
    }

    private void attackBossIfInRange(Npc boss) {
        if (boss.isVisible() && !hasRecentHitsplat(boss) && boss.interact("Attack")) {
            log.info("Attacking the boss from a safe distance.");
            Execution.delayUntil(() -> hasRecentHitsplat(boss), 300, 500);
        }
    }

    // Checks if the boss has any recent hitsplats
    private boolean hasRecentHitsplat(Npc boss) {
        return boss.getHitsplats().stream()
                .anyMatch(hitsplat -> hitsplat.isValid() && hitsplat.getClassification().isPlayers());
    }
}
