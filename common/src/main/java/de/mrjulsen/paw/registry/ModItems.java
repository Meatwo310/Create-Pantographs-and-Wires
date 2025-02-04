package de.mrjulsen.paw.registry;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.tterrag.registrate.util.entry.ItemEntry;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.item.PantographItem;
import de.mrjulsen.wires.item.WireBaseItem;
import net.minecraft.world.item.CreativeModeTabs;

public class ModItems {
    public static final ItemEntry<WireBaseItem> CATENARY_WIRE = PantographsAndWires.REGISTRATE.item("catenary_wire", p -> new WireBaseItem(p, ModWireRegistry.CATENARY_WIRE))
        .properties(p -> p)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();

    public static final ItemEntry<WireBaseItem> ENERGY_WIRE = PantographsAndWires.REGISTRATE.item("power_wire", p -> new WireBaseItem(p, ModWireRegistry.ENERGY_WIRE))
        .properties(p -> p)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();

    public static final ItemEntry<PantographItem> PANTOGRAPH = PantographsAndWires.REGISTRATE.item("pantograph", p -> PantographItem.create(ModBlocks.PANTOGRAPH.get(), p, false))
        .properties(p -> p)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();


        

    public static final ItemEntry<PantographItem> MOD_ICON = PantographsAndWires.REGISTRATE.item("mod_icon", p -> PantographItem.create(ModBlocks.PANTOGRAPH.get(), p, true))
        .removeTab(CreativeModeTabs.SEARCH)
        .register();

    public static final ItemEntry<SequencedAssemblyItem> CANTILEVER_GREEN_INCOMPLETE = PantographsAndWires.REGISTRATE.item("cantilever_incomplete", SequencedAssemblyItem::new)
        .removeTab(CreativeModeTabs.SEARCH)
        .register();

    
    public static void init() {
    }
}
