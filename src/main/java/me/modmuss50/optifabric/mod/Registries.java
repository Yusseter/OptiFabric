package me.modmuss50.optifabric.mod;

import net.minecraft.block.Block;
import net.minecraft.util.Identifier;

public class Registries {
	public static Identifier getID(Block block) {
		return net.minecraft.registry.Registries.BLOCK.getId(block);
	}
}
