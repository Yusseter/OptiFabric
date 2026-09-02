package me.modmuss50.optifabric.patcher.fixes;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.modmuss50.optifabric.util.RemappingUtils;

public class BedrockifyInGameHudFix implements ClassFixer {
    private static final String IN_GAME_HUD = "class_329";
    private static final String RENDER_HELD_ITEM_TOOLTIP = "method_1749";
    private static final String RENDER_HELD_ITEM_TOOLTIP_DESC = "(Lnet/minecraft/class_332;)V";

    @Override
    public void fix(ClassNode optifine, ClassNode minecraft) {
        String methodName = RemappingUtils.getMethodName(
                IN_GAME_HUD,
                RENDER_HELD_ITEM_TOOLTIP,
                RENDER_HELD_ITEM_TOOLTIP_DESC
        );
        String methodDesc = RemappingUtils.mapMethodDescriptor(RENDER_HELD_ITEM_TOOLTIP_DESC);

        MethodNode vanilla = findMethod(minecraft, methodName, methodDesc);
        if (vanilla == null) {
            throw new IllegalStateException("Could not find vanilla InGameHud.renderHeldItemTooltip");
        }

        for (int i = 0; i < optifine.methods.size(); i++) {
            MethodNode method = optifine.methods.get(i);
            if (methodName.equals(method.name) && methodDesc.equals(method.desc)) {
                optifine.methods.set(i, cloneMethod(vanilla));
                return;
            }
        }

        throw new IllegalStateException("Could not find OptiFine InGameHud.renderHeldItemTooltip");
    }

    private static MethodNode findMethod(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && desc.equals(method.desc)) {
                return method;
            }
        }

        return null;
    }

    private static MethodNode cloneMethod(MethodNode source) {
        String[] exceptions = source.exceptions == null
                ? null
                : source.exceptions.toArray(new String[source.exceptions.size()]);

        MethodNode copy = new MethodNode(
                source.access,
                source.name,
                source.desc,
                source.signature,
                exceptions
        );
        source.accept(copy);
        return copy;
    }
}
