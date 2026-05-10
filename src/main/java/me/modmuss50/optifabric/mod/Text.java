package me.modmuss50.optifabric.mod;

import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

public class Text {
	public static MutableText literal(String text) {
		return net.minecraft.text.Text.literal(text);
	}

	public static MutableText literal(String text, Formatting style) {
		return literal(text).formatted(style);
	}
}
