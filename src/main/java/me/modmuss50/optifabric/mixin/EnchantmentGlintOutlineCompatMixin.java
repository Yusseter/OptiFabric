package me.modmuss50.optifabric.mixin;

import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.enchantoutline.util.QuadHelper", remap = false)
public class EnchantmentGlintOutlineCompatMixin {
    @Unique
    private static final ThreadLocal<Sprite> optifabric$sourceSprite =
            new ThreadLocal<>();

    @Redirect(
            method = "thickenQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_777;comp_3723()Lnet/minecraft/class_2350;",
                    remap = false
            ),
            remap = false
    )
    private static Direction optifabric$captureSourceSprite(BakedQuad quad) {
        Sprite sprite = quad.sprite();

        if (sprite == null) {
            optifabric$sourceSprite.remove();
        } else {
            optifabric$sourceSprite.set(sprite);
        }

        return quad.face();
    }

    @ModifyArg(
            method = "thickenQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_777;<init>(Lorg/joml/Vector3fc;Lorg/joml/Vector3fc;Lorg/joml/Vector3fc;Lorg/joml/Vector3fc;JJJJILnet/minecraft/class_2350;Lnet/minecraft/class_1058;ZI)V",
                    remap = false
            ),
            index = 10,
            remap = false
    )
    private static Sprite optifabric$restoreSourceSprite(Sprite sprite) {
        Sprite sourceSprite = optifabric$sourceSprite.get();
        optifabric$sourceSprite.remove();

        return sourceSprite != null ? sourceSprite : sprite;
    }
}
