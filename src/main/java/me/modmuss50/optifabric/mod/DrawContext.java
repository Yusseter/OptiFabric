package me.modmuss50.optifabric.mod;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

public class DrawContext {
	public static int drawTextWithShadow(TextRenderer textRenderer, Object matrices, String text, int x, int y, int color) {
		if (matrices instanceof net.minecraft.client.gui.DrawContext) {
			net.minecraft.client.gui.DrawContext drawContext = (net.minecraft.client.gui.DrawContext) matrices;
			drawContext.drawTextWithShadow(textRenderer, text, x, y, color);
			return 0;
		}

		try {
			return (int) TextRenderer.class.getMethod("drawWithShadow", MatrixStack.class, String.class, int.class, int.class, int.class)
					.invoke(textRenderer, matrices, text, x, y, color);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to draw text with shadow", e);
		}
	}
}
