package com.fox2code.foxloader.registry;

import com.fox2code.foxloader.loader.ModContainer;
import com.fox2code.foxloader.loader.ModLoader;
import com.fox2code.foxloader.loader.lua.LuaInterop;

import java.util.*;

public abstract class GameRegistry {
    static final HashMap<String, RegistryEntry> registryEntries = new HashMap<>();
    static final HashMap<String, EntityTypeRegistryEntry> entityTypeEntries = new HashMap<>();
    static final BlockBuilder DEFAULT_BLOCK_BUILDER = new BlockBuilder();
    static final ItemBuilder DEFAULT_ITEM_BUILDER = new ItemBuilder();
    private static GameRegistry gameRegistry;
    public static final int PARAM_ITEM_ID_DIFF = 256;
    public static final int INITIAL_BLOCK_ID = 360;
    public static final int MAXIMUM_BLOCK_ID = 1024; // Hard max: 1258
    public static final int INITIAL_ITEM_ID = 4096;
    public static final int MAXIMUM_ITEM_ID = 8192; // Hard max: 31999
    public static final int INITIAL_ENTITY_TYPE_ID = 210;
    // Array size limit is fuzzy so lets avoid it.
    public static final int MAXIMUM_ENTITY_TYPE_ID = 255; // Integer.MAX_VALUE - Short.MAX_VALUE;
    // Block ids but translated to item ids
    public static final int INITIAL_TRANSLATED_BLOCK_ID = convertBlockIdToItemId(INITIAL_BLOCK_ID);
    public static final int MAXIMUM_TRANSLATED_BLOCK_ID = convertBlockIdToItemId(MAXIMUM_BLOCK_ID);
    // The default fallback id for blocks is stone.
    public static final int DEFAULT_FALLBACK_BLOCK_ID = 1;
    // The default fallback id for items is planks.
    public static final int DEFAULT_FALLBACK_ITEM_ID = 5;
    // The default fallback id for entity types is pig.
    public static final int DEFAULT_FALLBACK_ENTITY_TYPE_ID = 90;

    public static GameRegistry getInstance() {
        return gameRegistry;
    }

    private static final ThreadLocal<int[]> blockIntArrayLocal =
            ThreadLocal.withInitial(() -> new int[gameRegistry.getMaxBlockId() + 1]);

    /**
     * @return array to be temporary used in block calculations.
     */
    public static int[] getTemporaryBlockIntArray() {
        if (gameRegistry.isFrozen()) {
            int[] array = blockIntArrayLocal.get();
            Arrays.fill(array, 0);
            return array;
        }
        return new int[gameRegistry.getMaxBlockId() + 1];
    }

    /**
     * This is instanced by the mod loaded and should just be a static interface to interact with the game.
     */
    GameRegistry() {
        if (gameRegistry != null)
            throw new IllegalStateException("Only one registry can exists at a time");
        gameRegistry = this;
    }

    /**
     * @return a registered modded item with the corresponding registry name
     */
    @LuaInterop
    public RegisteredItem getRegisteredItem(String name) {
        RegistryEntry registryEntry = registryEntries.get(name);
        return registryEntry == null ? null :
                this.getRegisteredItem(registryEntry.realId);
    }

    /**
     * @return a registered modded item with the corresponding registry name
     */
    @LuaInterop
    public RegisteredBlock getRegisteredBlock(String name) {
        RegistryEntry registryEntry = registryEntries.get(name);
        return registryEntry == null ? null :
                this.getRegisteredBlock(
                        convertItemIdToBlockId(registryEntry.realId));
    }

    /**
     * @return list of registered modded block and item entries
     */
    public static Collection<RegistryEntry> getRegistryEntries() {
        return Collections.unmodifiableCollection(registryEntries.values());
    }

    /**
     * @return list of registered modded entities
     */
    public static Collection<EntityTypeRegistryEntry> getEntityRegistryEntries() {
        return Collections.unmodifiableCollection(entityTypeEntries.values());
    }

    /**
     * @return maximum expected block id
     */
    @LuaInterop
    public abstract int getMaxBlockId();

    /**
     * @return a registered item with the corresponding id
     */
    @LuaInterop
    public abstract RegisteredItem getRegisteredItem(int id);

    /**
     * @return a registered block with the corresponding id
     */
    @LuaInterop
    public abstract RegisteredBlock getRegisteredBlock(int id);

    /**
     * @param translationKey translation key to use
     * @return translated component
     */
    @LuaInterop
    public abstract String translateKey(String translationKey);

    /**
     * @param translationKey translation key to use
     * @param args translation arguments to use
     * @return translated formatted component
     */
    public abstract String translateKeyFormat(String translationKey, String... args);

    /**
     * Only use this if you know what you are doing.
     */
    public abstract int generateNewBlockId(String name, int fallbackId);

    /**
     * Only use this if you know what you are doing.
     */
    public abstract int generateNewItemId(String name, int fallbackId);

    /**
     * Only use this if you know what you are doing.
     */
    public abstract int generateNewEntityTypeId(String name, int fallbackId);

    /**
     * Register a new block into the game
     */
    public final RegisteredBlock registerNewBlock(String name, BlockBuilder blockBuilder) {
        return this.registerNewBlock(name, blockBuilder, DEFAULT_FALLBACK_BLOCK_ID);
    }

    public abstract RegisteredBlock registerNewBlock(String name, BlockBuilder blockBuilder, int fallbackId);

    /**
     * Register a new item into the game
     */
    public final RegisteredItem registerNewItem(String name, ItemBuilder itemBuilder) {
        return this.registerNewItem(name, itemBuilder, DEFAULT_FALLBACK_ITEM_ID);
    }

    public abstract RegisteredItem registerNewItem(String name, ItemBuilder itemBuilder, int fallbackId);

	/**
	 * Register a new entity type into the game
	 */
	public final void registerNewEntityType(String name, Class<? extends RegisteredEntity> entityClass) {
		this.registerNewEntityType(name, entityClass, DEFAULT_FALLBACK_ENTITY_TYPE_ID);
	}

	public abstract void registerNewEntityType(String name, Class<? extends RegisteredEntity> entityClass, int fallbackId);

    protected static final String LATE_RECIPE_MESSAGE = "Too late to register recipes!";
    protected static boolean recipeFrozen = false;

    public abstract void registerRecipe(RegisteredItemStack result, Object... recipe);

    public abstract void registerShapelessRecipe(RegisteredItemStack result, Ingredient... ingredients);

    public abstract void registerFurnaceRecipe(RegisteredItem input, RegisteredItemStack output);

    public abstract void registerBlastFurnaceRecipe(RegisteredItem input, RegisteredItemStack output);

    public abstract void registerFreezerRecipe(RegisteredItem input, RegisteredItemStack output);

    /**
     * @deprecated replace with {@link #registerFurnaceRecipe(RegisteredItem, RegisteredItemStack)}
     */
    @Deprecated
    public void addFurnaceRecipe(RegisteredItem input, RegisteredItemStack output) {
        this.registerFurnaceRecipe(input, output);
    }

    /**
     * @deprecated replace with {@link #registerBlastFurnaceRecipe(RegisteredItem, RegisteredItemStack)}
     */
    @Deprecated
    public void addBlastFurnaceRecipe(RegisteredItem input, RegisteredItemStack output) {
        this.registerBlastFurnaceRecipe(input, output);
    }

    /**
     * @deprecated replace with {@link #registerFreezerRecipe(RegisteredItem, RegisteredItemStack)}
     */
    @Deprecated
    public void addFreezerRecipe(RegisteredItem input, RegisteredItemStack output) {
        this.registerFreezerRecipe(input, output);
    }

    @LuaInterop
    public boolean isFrozen() {
        return ModLoader.areAllModsLoaded();
    }

    @LuaInterop
    public static int convertBlockIdToItemId(int blockID) {
        return blockID > 255 ? blockID + 744 : blockID;
    }

    @LuaInterop
    public static int convertItemIdToBlockId(int itemId) {
        return itemId > MAXIMUM_TRANSLATED_BLOCK_ID ? -1 : // -1 means no block equivalent.
                itemId > 255 ? itemId < 1000 ? -1 : itemId - 744 : itemId;
    }

    public static String validateAndFixRegistryName(String name) {
        if (name.indexOf(':') == -1 && !ModLoader.areAllModsLoaded()) {
            ModContainer modContainer = ModContainer.getActiveModContainer();
            if (modContainer != null) {
                name = modContainer.id + ":" + name;
            }
        }
        validateRegistryName(name);
        return name;
    }

    public static void validateRegistryName(String name) {
        if (name.indexOf(':') == -1) {
            throw new IllegalArgumentException("Please add your mod id in the registry name, ex \"modid:item\")");
        }
        if (name.indexOf('\0') != -1 || name.indexOf(' ') != -1) {
            throw new IllegalArgumentException("Null bytes and spaces are not supported in registry identifiers");
        }
    }

    /**
     * @param itemId the item id
     * @return if the id is an item id reserved for mod loader use
     */
    public static boolean isLoaderReservedItemId(int itemId) {
        return (itemId >= INITIAL_TRANSLATED_BLOCK_ID && itemId < MAXIMUM_TRANSLATED_BLOCK_ID)
                || (itemId >= INITIAL_ITEM_ID && itemId < MAXIMUM_ITEM_ID);
    }

    /**
     * @param itemId the item id
     * @return if the id is a block id reserved for mod loader use
     */
    public static boolean isLoaderReservedBlockItemId(int itemId) {
        return itemId >= INITIAL_TRANSLATED_BLOCK_ID && itemId < MAXIMUM_TRANSLATED_BLOCK_ID;
    }

    /**
     * @param entityTypeId the entity type id
     * @return if the id is an entity id reserved for mod loader use
     */
    public static boolean isLoaderReservedEntityTypeId(int entityTypeId) {
        return entityTypeId >= INITIAL_ENTITY_TYPE_ID && entityTypeId < MAXIMUM_ENTITY_TYPE_ID;
    }

    public interface Ingredient {}

    public enum BuiltInMaterial implements EnumReflectTranslator.ReflectEnum {
        AIR, GRASS("grass", "grassMaterial"), GROUND, WOOD, ROCK,
        IRON, WATER, LAVA, LEAVES, PLANTS, SPONGE, CLOTH, FIRE,
        SAND, CIRCUITS, GLASS, TNT, CORAL, ICE, SNOW, BUILT_SNOW("builtSnow"),
        CACTUS, CLAY, PUMPKIN, PORTAL, CAKE("cakeMaterial"),
        WEB("web", "cobweb"), PISTON, CHAIR, QUICKSAND, ASH,
        MOVEABLE_CIRCUIT("moveableCircuit"), LIGHT_BLOCK("lightBlock"),
        SUGAR_CANE("sugarCane"), HONEYCOMB, OBSIDIAN, MAGMA,
        POTION_FIRE("potionfire");

        private final String[] reflectNames;

        BuiltInMaterial() {
            this.reflectNames = new String[]{name().toLowerCase(Locale.ROOT)};
        }

        BuiltInMaterial(String... reflectNames) {
            this.reflectNames = reflectNames;
        }

        @Override
        public String[] getReflectNames() {
            return this.reflectNames;
        }
    }

    public enum BuiltInStepSounds implements EnumReflectTranslator.ReflectEnum {
        POWDER("soundPowder", "soundPowderFootstep", /* Why? */ "soundUnused"),
        WOOD("soundWood", "soundWoodFootstep"),
        GRAVEL("soundGravel", "soundGravelFootstep"),
        GRASS("soundGrass", "soundGrassFootstep"),
        STONE("soundStone", "soundStoneFootstep"),
        METAL("soundMetal", "soundMetalFootstep"),
        GLASS("soundGlass", "soundGlassFootstep"),
        CLOTH("soundCloth", "soundClothFootstep"),
        SAND("soundSand", "soundSandFootstep"),
        BUSH("soundBush", "soundBushFootstep"),
        SNOW("soundSnow", "soundSnowFootstep"),
        SLIME("soundSlime", "soundSlimeFootstep");
        private final String[] reflectNames;

        BuiltInStepSounds(String... reflectNames) {
            this.reflectNames = reflectNames;
        }

        @Override
        public String[] getReflectNames() {
            return this.reflectNames;
        }
    }

    public enum BuiltInBlockType {
        CUSTOM, BLOCK, GLASS, WORKBENCH, FALLING, SLAB("_full"), STAIRS;

        public final String secRegistryExt;

        BuiltInBlockType(String secRegistryExt) {
            this.secRegistryExt = secRegistryExt;
        }

        BuiltInBlockType() {
            this.secRegistryExt = null;
        }
    }
}
