package com.patryk3211.fizite.block;

import com.patryk3211.fizite.block.cylinder.CopperCylinder;
import com.patryk3211.fizite.block.pipe.CopperPipe;
import io.wispforest.owo.registration.reflect.BlockRegistryContainer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public class AllBlocks implements BlockRegistryContainer {
    public static final Block COPPER_PIPE = new CopperPipe();

    public static final Block COPPER_CYLINDER = new CopperCylinder();

    public static final Block CONNECTING_ROD = new ConnectingRod();

    public static final Block CRANK_SHAFT = new CrankShaft();
}
