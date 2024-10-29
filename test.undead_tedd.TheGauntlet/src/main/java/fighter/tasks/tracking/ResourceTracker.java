package fighter.tasks.tracking;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import static fighter.DungeonUtils.getResourceTracker;

@Log4j2(topic = "ResourceTracker")
public class ResourceTracker {

    // Resource counts
    private int paddlefishCount;
    private int corruptedShards;
    private int crystallineShards;
    private int grymLeaf;
    private int vialCount;
    private int waterFilledVial;
    private int PotionCount;
    private int weaponFrameCount;
    private int corruptedOrbCount;
    private int crystallineOrbCount;
    private int corruptedBowstringCount;
    private int crystallineBowstringCount;
    private int corruptedDustCount;
    private int crystallineDustCount;
    private int crystalOre;
    private int corruptedOre;
    private int phrenBark;
    private int linumTirinium;


    // Crafting phase requirements
    private final int PHASE1_REQUIRED_SHARDS = 160; // Combined crystalline + corrupted

    private final int PHASE2_REQUIRED_SHARDS = 380; // Combined crystalline + corrupted
    private final int PHASE2_REQUIRED_CRYSTAL_ORE = 3;
    private final int PHASE2_REQUIRED_PHREN_BARK = 3;
    private final int PHASE2_REQUIRED_LINUM_TIRINIUM = 3;
    private final int PHASE2_REQUIRED_BOWSTRING = 1; // Combined crystalline + corrupted
    private final int PHASE2_REQUIRED_ORB = 1; // Combined crystalline + corrupted

    // Add to ResourceTracker.java

    // Getter for Phase 1 required shards
    public int getPhase1RequiredShards() {
        return PHASE1_REQUIRED_SHARDS;
    }

    // Method to check if all Phase 2 crafting requirements are met
    public boolean isPhaseTwoCraftingComplete() {
        int totalShards = crystallineShards + corruptedShards;
        int totalBowstrings = crystallineBowstringCount + corruptedBowstringCount;
        int totalOrbs = crystallineOrbCount + corruptedOrbCount;

        return totalShards >= PHASE2_REQUIRED_SHARDS &&
                totalBowstrings >= PHASE2_REQUIRED_BOWSTRING &&
                totalOrbs >= PHASE2_REQUIRED_ORB &&
                crystalOre >= PHASE2_REQUIRED_CRYSTAL_ORE &&
                linumTirinium >= PHASE2_REQUIRED_LINUM_TIRINIUM &&
                phrenBark >= PHASE2_REQUIRED_PHREN_BARK;
    }

    public synchronized void incrementResourceCount(String resourceName) {
        switch (resourceName) {
            case "Crystalline Shards":
                crystallineShards++;
                break;
            case "Corrupted Shards":
                corruptedShards++;
                break;
            case "Weapon Frame":
                weaponFrameCount++;
                break;
            case "Raw Paddlefish":
                paddlefishCount++;
                break;
            case "Grym Leaf":
                grymLeaf++;
                break;
            case "Crystal Ore":
                crystalOre++;
                break;
            case "Corrupted Ore":
                corruptedOre++;
                break;
            case "Linum Tirinium":
                linumTirinium++;
                break;
            case "Phren Bark":
                phrenBark++;
                break;
            // Add any other resources as needed
            default:
                log.warn("Attempted to increment unknown resource: " + resourceName);
                break;
        }
    }


    // Crafted items tracking
    private boolean basicBowCrafted;
    private boolean attunedBowCrafted;
    private boolean perfectedBowCrafted;
    private boolean basicStaffCrafted;
    private boolean attunedStaffCrafted;
    @Setter
    private boolean perfectedStaffCrafted;

    // Crafted items tracking for armor (body, legs, helm) with corrupted and crystal variants
    private boolean corruptedBodyCrafted;
    private boolean crystalBodyCrafted;
    private boolean corruptedLegsCrafted;
    private boolean crystalLegsCrafted;
    private boolean corruptedHelmCrafted;
    private boolean crystalHelmCrafted;

    // Current crafting phase
    @Getter
    private int craftingPhase = 1;

    // Method to check if ready for crafting phase 1 or 2
    public boolean isReadyToCraft() {
        int totalShards = crystallineShards + corruptedShards;
        int totalBowstrings = crystallineBowstringCount + corruptedBowstringCount;
        int totalOrbs = crystallineOrbCount + corruptedOrbCount;

        if (craftingPhase == 1) {
            int PHASE1_REQUIRED_WEAPON_FRAMES = 2;
            return totalShards >= PHASE1_REQUIRED_SHARDS &&
                    weaponFrameCount >= PHASE1_REQUIRED_WEAPON_FRAMES;
        } else if (craftingPhase == 2) {
            return totalShards >= PHASE2_REQUIRED_SHARDS &&
                    totalBowstrings >= PHASE2_REQUIRED_BOWSTRING &&
                    totalOrbs >= PHASE2_REQUIRED_ORB &&
                    crystalOre >= PHASE2_REQUIRED_CRYSTAL_ORE &&
                    linumTirinium >= PHASE2_REQUIRED_LINUM_TIRINIUM &&
                    phrenBark >= PHASE2_REQUIRED_PHREN_BARK;
        }
        return false;
    }

    // Advance to the next crafting phase if requirements are met
    public void advanceCraftingPhase() {
        if (craftingPhase == 1 && isReadyToCraft()) {
            craftingPhase = 2;
            log.info("Advanced to crafting phase 2.");
        } else if (craftingPhase == 2 && isReadyToCraft()) {
            log.info("Crafting phase 2 requirements met. All crafting phases complete.");
        } else {
            log.info("Crafting requirements for current phase not yet met.");
        }
    }

    // Check if specific resource is still needed (by quantity)
    public static boolean isNeededResource(String resourceName) {
        return switch (resourceName) {
            case "Raw Paddlefish" -> getResourceTracker().getPaddlefishCount() < 23;
            case "Grym Leaf" -> getResourceTracker().getGrymLeaf() < 3;
            case "Crystal Ore" -> getResourceTracker().getCrystalOre() < 3;
            case "Corrupted ore" -> getResourceTracker().getCorruptedOre() < 3;
            case "Phren Bark" -> getResourceTracker().getPhrenBark() < 3;
            case "Linum Tirinium" -> getResourceTracker().getLinumTirinium() < 3;
            case "Corrupted Shards" -> getResourceTracker().getCorruptedShards() < 380;
            case "Crystalline Shards" -> getResourceTracker().getCrystallineShards() < 380;
            case "Corrupted Orb" -> getResourceTracker().getCorruptedOrbCount() < 1;
            case "Crystalline Orb" -> getResourceTracker().getCrystallineOrbCount() < 1;
            case "Corrupted Bowstring" -> getResourceTracker().getCorruptedBowstringCount() < 1;
            case "Crystalline Bowstring" -> getResourceTracker().getCrystallineBowstringCount() < 1;
            case "Weapon Frame" -> getResourceTracker().getWeaponFrameCount() < 2;
            case "Corrupted Dust" -> getResourceTracker().getCorruptedDustCount() < 30;
            case "Crystalline Dust" -> getResourceTracker().getCrystallineDustCount() < 30;
            case "Vial" -> getResourceTracker().getVialCount() < 3;
            case "Water-Filled Vial" -> getResourceTracker().getWaterFilledVial() < 3;
            case "Egniol Potion (3)" -> getResourceTracker().getPotionCount() < 3;
            default -> false;
        };
    }

    public boolean isResourceFullyGathered(String resourceName) {
        return switch (resourceName) {
            case "Raw Paddlefish" -> getPaddlefishCount() >= 23;
            case "Grym Leaf" -> getGrymLeaf() >= 3;
            case "Crystal Ore" -> getCrystalOre() >= 3;
            case "Corrupted ore" -> getCorruptedOre() >= 3;
            case "Phren Bark" -> getPhrenBark() >= 3;
            case "Linum Tirinium" -> getLinumTirinium() >= 3;
            case "Corrupted Shards" -> getCorruptedShards() >= 380;
            case "Crystalline Shards" -> getCrystallineShards() >= 380;
            case "Corrupted Orb" -> getCorruptedOrbCount() >= 1;
            case "Crystalline Orb" -> getCrystallineOrbCount() >= 1;
            case "Corrupted Bowstring" -> getCorruptedBowstringCount() >= 1;
            case "Crystalline Bowstring" -> getCrystallineBowstringCount() >= 1;
            case "Weapon Frame" -> getWeaponFrameCount() >= 2;
            case "Corrupted Dust" -> getCorruptedDustCount() >= 30;
            case "Crystalline Dust" -> getCrystallineDustCount() >= 30;
            case "Vial" -> getVialCount() >= 3;
            case "Water-Filled Vial" -> getWaterFilledVial() >= 3;
            case "Egniol Potion (3)" -> getPotionCount() >= 3;
            default -> false;
        };
    }

    // Reset crafting progress for new run
    public void resetCraftingProgress() {
        resetCraftedItems();
    }

    // Reset method for new run
    public void resetForNewRun() {
        paddlefishCount = 0;
        corruptedShards = 0;
        crystallineShards = 0;
        grymLeaf = 0;
        vialCount = 0;
        weaponFrameCount = 0;
        crystalOre = 0;
        phrenBark = 0;
        linumTirinium = 0;
        corruptedOrbCount = 0;
        crystallineOrbCount = 0;
        corruptedBowstringCount = 0;
        crystallineBowstringCount = 0;
        corruptedDustCount = 0;
        crystallineDustCount = 0;

        craftingPhase = 1; // Reset to phase 1 for new run

        log.info("Resource tracker reset for new run. All resource counts set to zero.");
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

    public void incrementCrystallineShards() {
        crystallineShards++; // Adjust variable name as necessary
    }

    public void incrementCorruptedShards() {
        corruptedShards++;
    }

    public void incrementWeaponFrameCount() {
        weaponFrameCount++;
    }

    public void incrementPaddlefishCount() {
        paddlefishCount++;
    }

    public void incrementGrymLeaf() {
        grymLeaf++;
    }

    // Reset all crafted items (for new runs or when restarting)
    public synchronized void resetCraftedItems() {
        // Reset crafted bows
        basicBowCrafted = false;
        attunedBowCrafted = false;
        perfectedBowCrafted = false;

        // Reset crafted staffs
        basicStaffCrafted = false;
        attunedStaffCrafted = false;
        perfectedStaffCrafted = false;

        // Reset crafted armor
        corruptedBodyCrafted = false;
        crystalBodyCrafted = false;
        corruptedLegsCrafted = false;
        crystalLegsCrafted = false;
        corruptedHelmCrafted = false;
        crystalHelmCrafted = false;

        log.info("Reset crafted items progress for bows, staffs, and armor.");
    }

    public synchronized void trackArmorCrafting(String type, String variant) {
        switch (type.toLowerCase()) {
            case "body":
                if (variant.equalsIgnoreCase("corrupted")) {
                    corruptedBodyCrafted = true;
                    log.info("Corrupted Body Armor crafted.");
                } else if (variant.equalsIgnoreCase("crystal")) {
                    crystalBodyCrafted = true;
                    log.info("Crystal Body Armor crafted.");
                } else {
                    log.warn("Unknown body armor variant: " + variant);
                }
                break;

            case "legs":
                if (variant.equalsIgnoreCase("corrupted")) {
                    corruptedLegsCrafted = true;
                    log.info("Corrupted Leg Armor crafted.");
                } else if (variant.equalsIgnoreCase("crystal")) {
                    crystalLegsCrafted = true;
                    log.info("Crystal Leg Armor crafted.");
                } else {
                    log.warn("Unknown leg armor variant: " + variant);
                }
                break;

            case "helm":
                if (variant.equalsIgnoreCase("corrupted")) {
                    corruptedHelmCrafted = true;
                    log.info("Corrupted Helm crafted.");
                } else if (variant.equalsIgnoreCase("crystal")) {
                    crystalHelmCrafted = true;
                    log.info("Crystal Helm crafted.");
                } else {
                    log.warn("Unknown helm variant: " + variant);
                }
                break;

            default:
                log.warn("Unknown armor type: " + type);
        }
    }

    public synchronized boolean isArmorCrafted(String type, String variant) {
        return !switch (type.toLowerCase()) {
            case "body" -> variant.equalsIgnoreCase("corrupted") ? corruptedBodyCrafted :
                    variant.equalsIgnoreCase("crystal") && crystalBodyCrafted;
            case "legs" -> variant.equalsIgnoreCase("corrupted") ? corruptedLegsCrafted :
                    variant.equalsIgnoreCase("crystal") && crystalLegsCrafted;
            case "helm" -> variant.equalsIgnoreCase("corrupted") ? corruptedHelmCrafted :
                    variant.equalsIgnoreCase("crystal") && crystalHelmCrafted;
            default -> {
                log.warn("Unknown armor type or variant: " + type + ", " + variant);
                yield false;
            }
        };
    }


    // Getters for all resource types
    public synchronized int getPaddlefishCount() {
        return paddlefishCount;
    }

    public synchronized int getCrystalOre() {
        return crystalOre;
    }

    public synchronized int getPhrenBark() {
        return phrenBark;
    }

    public synchronized int getLinumTirinium() {
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

    public synchronized int getWaterFilledVial() {
        return waterFilledVial;
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

    public synchronized int getPotionCount() {
        return PotionCount;
    }

    public synchronized int getCorruptedOre() {
        return corruptedOre;
    }

    public boolean isBowCrafted(String tier) {
        return switch (tier.toLowerCase()) {
            case "basic" -> basicBowCrafted;
            case "attuned" -> attunedBowCrafted;
            case "perfected" -> perfectedBowCrafted;
            default -> false;
        };
    }

    public boolean isStaffCrafted(String tier) {
        return switch (tier.toLowerCase()) {
            case "basic" -> basicStaffCrafted;
            case "attuned" -> attunedStaffCrafted;
            case "perfected" -> perfectedStaffCrafted;
            default -> false;
        };
    }
}

