package fighter.tasks.tracking;

import lombok.extern.log4j.Log4j2;

import static fighter.DungeonUtils.DungeonUtils.*;

@Log4j2(topic = "ResourceTracker")
public class ResourceTracker {

    private int paddlefishCount;
    private int corruptedShards;
    private int crystallineShards;
    private int grymLeaf;
    private int vialCount;
    private int weaponFrameCount;
    private int corruptedOrbCount;
    private int crystallineOrbCount;
    private int corruptedBowstringCount;
    private int crystallineBowstringCount;
    private int corruptedDustCount;
    private int crystallineDustCount;

    // Additional resource fields
    private int crystalOre;
    private int phrenBark;
    private int linumTirinium;

    private int craftingPhase = 1; // Default to phase 1
    // New Fields for Tracking Crafted Items
    private boolean basicBowCrafted;
    private boolean attunedBowCrafted;
    private boolean perfectedBowCrafted;

    private boolean basicStaffCrafted;
    private boolean attunedStaffCrafted;
    private boolean perfectedStaffCrafted;


    // Method to check if ready for crafting phase 1 or 2
    public boolean isReadyToCraft() {
        if (craftingPhase == 1) {
            return crystallineShards >= 160 && weaponFrameCount >= 2;
        } else if (craftingPhase == 2) {
            return crystallineShards >= 380 &&
                    weaponFrameCount >= 2 &&
                    crystalOre >= 3 &&
                    linumTirinium >= 3 &&
                    phrenBark >= 3;
        }
        return false;
    }

    public static boolean isNeededResource(String resourceName) {
        switch (resourceName) {
            case "Grym Leaf":
                return getResourceTracker().getGrymLeafCount() < 3;
            case "Crystal Ore":
                return getResourceTracker().getCrystalOreCount() < 3;
            case "Phren Bark":
                return getResourceTracker().getPhrenBarkCount() < 3;
            case "Linum Tirinium":
                return getResourceTracker().getLinumTiriniumCount() < 3;
            case "Corrupted Shards":
                return getResourceTracker().getCorruptedShards() < 380;
            case "Crystalline Shards":
                return getResourceTracker().getCrystallineShards() < 380;
            case "Corrupted Orb":
                return getResourceTracker().getCorruptedOrbCount() < 1;
            case "Crystalline Orb":
                return getResourceTracker().getCrystallineOrbCount() < 1;
            case "Corrupted Bowstring":
                return getResourceTracker().getCorruptedBowstringCount() < 1;
            case "Crystalline Bowstring":
                return getResourceTracker().getCrystallineBowstringCount() < 1;
            case "Weapon Frame":
                return getResourceTracker().getWeaponFrameCount() < 2;
            case "Corrupted Dust":
                return getResourceTracker().getCorruptedDustCount() < 30;
            case "Crystalline Dust":
                return getResourceTracker().getCrystallineDustCount() < 30;
            default:
                return false;
        }
    }
    // Reset crafting progress for a new run
    public void resetCraftingProgress() {
        resetCraftedItems();
    }

    public boolean isReadyForFirstCraft() {
        return getCrystallineShards() >= 150 && getWeaponFrameCount() >= 2;
    }

    public boolean isReadyForSecondCraft() {
        return getCrystallineShards() >= 380 &&
                getLinumTiriniumCount() >= 1 && getPhrenBarkCount() >= 1;
    }


    // Advance to the next crafting phase
    public void advanceCraftingPhase() {
        if (craftingPhase == 1) {
            craftingPhase = 2;
            log.info("Advanced to second crafting phase.");
        }
    }

    // Reset after second crafting phase
    public void resetAfterCrafting() {
        craftingPhase = 1; // Reset to phase 1 for new runs
        reset(); // Reset all resources
        log.info("Reset crafting progress and resources for new run.");
    }

    public void trackResource(String resourceName) {
        switch (resourceName) {
            case "Crystal Ore":
                trackCrystalOre(1);
                break;
            case "Phren Bark":
                trackPhrenBark(1);
                break;
            case "Linum Tirinium":
                trackLinumTirinium(1);
                break;
            case "Grym Leaf":
                trackGrymLeaf(1);
                break;
            case "Raw Paddlefish":
                trackPaddlefish(1);
                break;
            case "Corrupted Shards":
                trackCorruptedShards(1);
                break;
            case "Crystalline Shards":
                trackCrystallineShards(1);
                break;
            default:
                log.warn("Unknown resource: " + resourceName);
                break;
        }
    }

    // Paddlefish tracking
    public synchronized void trackPaddlefish(int count) {
        paddlefishCount = Math.max(0, paddlefishCount + count); // Ensure no negative values
        log.info("Tracked {} paddlefish. Total now: {}", count, paddlefishCount);
    }

    // Crystal Ore tracking
    public synchronized void trackCrystalOre(int count) {
        crystalOre = Math.max(0, crystalOre + count); // Ensure no negative values
        log.info("Tracked {} crystal ore. Total now: {}", count, crystalOre);
    }

    // Phren Bark tracking
    public synchronized void trackPhrenBark(int count) {
        phrenBark = Math.max(0, phrenBark + count); // Ensure no negative values
        log.info("Tracked {} phren bark. Total now: {}", count, phrenBark);
    }

    // Linum Tirinium tracking
    public synchronized void trackLinumTirinium(int count) {
        linumTirinium = Math.max(0, linumTirinium + count); // Ensure no negative values
        log.info("Tracked {} linum tirinium. Total now: {}", count, linumTirinium);
    }

    // Corrupted Shards tracking
    public synchronized void trackCorruptedShards(int count) {
        corruptedShards = Math.max(0, corruptedShards + count); // Ensure no negative values
        log.info("Tracked {} corrupted shards. Total now: {}", count, corruptedShards);
    }

    // Crystalline Shards tracking
    public synchronized void trackCrystallineShards(int count) {
        crystallineShards = Math.max(0, crystallineShards + count); // Ensure no negative values
        log.info("Tracked {} crystalline shards. Total now: {}", count, crystallineShards);
    }

    // Grym Leaf tracking
    public synchronized void trackGrymLeaf(int count) {
        grymLeaf = Math.max(0, grymLeaf + count); // Ensure no negative values
        log.info("Tracked {} grym leaves. Total now: {}", count, grymLeaf);
    }

    // Vial tracking
    public synchronized void trackVials(int count) {
        vialCount = Math.max(0, vialCount + count); // Ensure no negative values
        log.info("Tracked {} vials. Total now: {}", count, vialCount);
    }

    // Weapon Frame tracking
    public synchronized void trackWeaponFrame(int count) {
        weaponFrameCount = Math.max(0, weaponFrameCount + count);
        log.info("Tracked {} weapon frame(s). Total now: {}", count, weaponFrameCount);
    }

    // Corrupted Orb tracking
    public synchronized void trackCorruptedOrb(int count) {
        corruptedOrbCount = Math.max(0, corruptedOrbCount + count);
        log.info("Tracked {} corrupted orb(s). Total now: {}", count, corruptedOrbCount);
    }

    // Crystalline Orb tracking
    public synchronized void trackCrystallineOrb(int count) {
        crystallineOrbCount = Math.max(0, crystallineOrbCount + count);
        log.info("Tracked {} crystalline orb(s). Total now: {}", count, crystallineOrbCount);
    }

    // Corrupted Bowstring tracking
    public synchronized void trackCorruptedBowstring(int count) {
        corruptedBowstringCount = Math.max(0, corruptedBowstringCount + count);
        log.info("Tracked {} corrupted bowstring(s). Total now: {}", count, corruptedBowstringCount);
    }

    // Crystalline Bowstring tracking
    public synchronized void trackCrystallineBowstring(int count) {
        crystallineBowstringCount = Math.max(0, crystallineBowstringCount + count);
        log.info("Tracked {} crystalline bowstring(s). Total now: {}", count, crystallineBowstringCount);
    }

    // Corrupted Dust tracking
    public synchronized void trackCorruptedDust(int count) {
        corruptedDustCount = Math.max(0, corruptedDustCount + count);
        log.info("Tracked {} corrupted dust. Total now: {}", count, corruptedDustCount);
    }

    // Crystalline Dust tracking
    public synchronized void trackCrystallineDust(int count) {
        crystallineDustCount = Math.max(0, crystallineDustCount + count);
        log.info("Tracked {} crystalline dust. Total now: {}", count, crystallineDustCount);
    }

    // Getters for all resource types
    public synchronized int getPaddlefishCount() {
        return paddlefishCount;
    }

    public synchronized int getCrystalOreCount() {
        return crystalOre;
    }

    public synchronized int getPhrenBarkCount() {
        return phrenBark;
    }

    public synchronized int getLinumTiriniumCount() {
        return linumTirinium;
    }

    public synchronized int getCorruptedShards() {
        return corruptedShards;
    }

    public synchronized int getCrystallineShards() {
        return crystallineShards;
    }

    public synchronized int getGrymLeaf() {
        return grymLeaf;
    }

    public synchronized int getVialCount() {
        return vialCount;
    }

    public synchronized int getWeaponFrameCount() {
        return weaponFrameCount;
    }

    public synchronized int getCorruptedOrbCount() {
        return corruptedOrbCount;
    }

    public synchronized int getCrystallineOrbCount() {
        return crystallineOrbCount;
    }

    public synchronized int getCorruptedBowstringCount() {
        return corruptedBowstringCount;
    }

    public synchronized int getCrystallineBowstringCount() {
        return crystallineBowstringCount;
    }

    public synchronized int getCorruptedDustCount() {
        return corruptedDustCount;
    }

    public synchronized int getCrystallineDustCount() {
        return crystallineDustCount;
    }

    public synchronized int getGrymLeafCount() {
        return grymLeaf;
    }

    // Reset all resource counts
    public synchronized void reset() {
        paddlefishCount = 0;
        crystalOre = 0;
        phrenBark = 0;
        linumTirinium = 0;
        corruptedShards = 0;
        crystallineShards = 0;
        grymLeaf = 0;
        vialCount = 0;
        weaponFrameCount = 0;
        corruptedOrbCount = 0;
        crystallineOrbCount = 0;
        corruptedBowstringCount = 0;
        crystallineBowstringCount = 0;
        corruptedDustCount = 0;
        crystallineDustCount = 0;
        log.info("All resource counts have been reset.");
    }

    // Set the exact counts if needed (e.g., after looting exact amounts)
    public synchronized void setPaddlefishCount(int count) {
        paddlefishCount = Math.max(0, count);
        log.info("Set paddlefish count to {}", paddlefishCount);
    }

    public synchronized void setCrystalOreCount(int count) {
        crystalOre = Math.max(0, count);
        log.info("Set crystal ore count to {}", crystalOre);
    }

    public synchronized void setPhrenBarkCount(int count) {
        phrenBark = Math.max(0, count);
        log.info("Set phren bark count to {}", phrenBark);
    }

    public synchronized void setLinumTiriniumCount(int count) {
        linumTirinium = Math.max(0, count);
        log.info("Set linum tirinium count to {}", linumTirinium);
    }

    public synchronized void setCorruptedShardsCount(int count) {
        corruptedShards = Math.max(0, count);
        log.info("Set corrupted shards count to {}", corruptedShards);
    }

    public synchronized void setCrystallineShardsCount(int count) {
        crystallineShards = Math.max(0, count);
        log.info("Set crystalline shards count to {}", crystallineShards);
    }

    public synchronized void setGrymLeafCount(int count) {
        grymLeaf = Math.max(0, count);
        log.info("Set grym leaves count to {}", grymLeaf);
    }

    public synchronized void setVialCount(int count) {
        vialCount = Math.max(0, count);
        log.info("Set vial count to {}", vialCount);
    }

    public synchronized void setWeaponFrameCount(int count) {
        weaponFrameCount = Math.max(0, count);
        log.info("Set weapon frame count to {}", weaponFrameCount);
    }

    public synchronized void setCorruptedOrbCount(int count) {
        corruptedOrbCount = Math.max(0, count);
        log.info("Set corrupted orb count to {}", corruptedOrbCount);
    }

    public synchronized void setCrystallineOrbCount(int count) {
        crystallineOrbCount = Math.max(0, count);
        log.info("Set crystalline orb count to {}", crystallineOrbCount);
    }

    public synchronized void setCorruptedBowstringCount(int count) {
        corruptedBowstringCount = Math.max(0, count);
        log.info("Set corrupted bowstring count to {}", corruptedBowstringCount);
    }

    public synchronized void setCrystallineBowstringCount(int count) {
        crystallineBowstringCount = Math.max(0, count);
        log.info("Set crystalline bowstring count to {}", crystallineBowstringCount);
    }

    public synchronized void setCorruptedDustCount(int count) {
        corruptedDustCount = Math.max(0, count);
        log.info("Set corrupted dust count to {}", corruptedDustCount);
    }

    public synchronized void setCrystallineDustCount(int count) {
        crystallineDustCount = Math.max(0, count);
        log.info("Set crystalline dust count to {}", crystallineDustCount);
    }

    public static int getBowstringCount(String type) {
        if (type.equals("corrupted")) {
            return getResourceTracker().getCorruptedBowstringCount();
        } else if (type.equals("crystalline")) {
            return getResourceTracker().getCrystallineBowstringCount();
        }
        return 0;
    }

    // Tracking crafted bow tiers
    public synchronized void trackBowCrafting(String tier) {
        switch (tier.toLowerCase()) {
            case "basic":
                basicBowCrafted = true;
                log.info("Basic Bow crafted.");
                break;
            case "attuned":
                attunedBowCrafted = true;
                log.info("Attuned Bow crafted.");
                break;
            case "perfected":
                perfectedBowCrafted = true;
                log.info("Perfected Bow crafted.");
                break;
            default:
                log.warn("Unknown bow tier: " + tier);
        }
    }

    // Tracking crafted staff tiers
    public synchronized void trackStaffCrafting(String tier) {
        switch (tier.toLowerCase()) {
            case "basic":
                basicStaffCrafted = true;
                log.info("Basic Staff crafted.");
                break;
            case "attuned":
                attunedStaffCrafted = true;
                log.info("Attuned Staff crafted.");
                break;
            case "perfected":
                perfectedStaffCrafted = true;
                log.info("Perfected Staff crafted.");
                break;
            default:
                log.warn("Unknown staff tier: " + tier);
        }
    }

    // Getters to check if a bow of a specific tier is crafted
    public synchronized boolean isBowCrafted(String tier) {
        return switch (tier.toLowerCase()) {
            case "basic" -> basicBowCrafted;
            case "attuned" -> attunedBowCrafted;
            case "perfected" -> perfectedBowCrafted;
            default -> {
                log.warn("Unknown bow tier: " + tier);
                yield false;
            }
        };
    }

    // Getters to check if a staff of a specific tier is crafted
    public synchronized boolean isStaffCrafted(String tier) {
        return switch (tier.toLowerCase()) {
            case "basic" -> basicStaffCrafted;
            case "attuned" -> attunedStaffCrafted;
            case "perfected" -> perfectedStaffCrafted;
            default -> {
                log.warn("Unknown staff tier: " + tier);
                yield false;
            }
        };
    }

    // Reset all crafted items (for new runs or when restarting)
    public synchronized void resetCraftedItems() {
        basicBowCrafted = false;
        attunedBowCrafted = false;
        perfectedBowCrafted = false;

        basicStaffCrafted = false;
        attunedStaffCrafted = false;
        perfectedStaffCrafted = false;

        log.info("Reset crafted items progress for bows and staffs.");
    }

    public boolean hasGatheredAll(String resourceName) {
        // Example logic: Check if all required resources of a specific type are gathered
        return switch (resourceName) {
            case "Pickaxe" -> getCrystalOreCount() >= REQUIRED_ORE;
            case "Hatchet" -> getPhrenBarkCount() >= REQUIRED_BARK;
            case "Hammer" -> getLinumTiriniumCount() >= REQUIRED_TIRINIUM;
            default -> false; // Default to false if resource is not recognized
        };
    }
}