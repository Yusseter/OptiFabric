package me.modmuss50.optifabric.patcher.fixes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Called from FluidRendererFix at runtime so game and Fabric classes are not
 * loaded while OptiFine classes are being patched.
 */
public class FluidRendererFixExternal {
    private static volatile Method fluidColorMethod;

    public static int getFabricColor(
            Object level,
            Object pos,
            Object state,
            int original
    ) {
        Object handler = getCurrentHandler();

        if (handler == null || isDefaultFabricHandler(handler)) {
            return original;
        }

        try {
            int color =
                    ((Number) getFluidColorMethod().invoke(
                            handler,
                            level,
                            pos,
                            state
                    )).intValue();

            return color & 0xFFFFFF;
        } catch (
                IllegalAccessException |
                InvocationTargetException e
        ) {
            throw new IllegalStateException(
                    "Unable to invoke Fabric fluid color handler",
                    e
            );
        }
    }

    private static Method getFluidColorMethod() {
        Method method = fluidColorMethod;

        if (method != null) {
            return method;
        }

        synchronized (FluidRendererFixExternal.class) {
            method = fluidColorMethod;

            if (method != null) {
                return method;
            }

            try {
                Class<?> handlerInterface =
                        Class.forName(
                                "net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler"
                        );

                Method match = null;

                for (Method candidate : handlerInterface.getMethods()) {
                    if (
                            !"getFluidColor".equals(candidate.getName()) ||
                            candidate.getParameterCount() != 3 ||
                            candidate.getReturnType() != int.class
                    ) {
                        continue;
                    }

                    if (match != null) {
                        throw new IllegalStateException(
                                "Found multiple Fabric fluid color methods"
                        );
                    }

                    match = candidate;
                }

                if (match == null) {
                    throw new IllegalStateException(
                            "Could not find Fabric fluid color method"
                    );
                }

                fluidColorMethod = match;
                return match;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Unable to resolve Fabric fluid render handler",
                        e
                );
            }
        }
    }

    private static Object getCurrentHandler() {
        try {
            Class<?> renderingImpl =
                    Class.forName(
                            "net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderingImpl"
                    );

            Method getCurrentInfo =
                    renderingImpl.getMethod(
                            "getCurrentInfo"
                    );

            Object info =
                    getCurrentInfo.invoke(null);

            Field handler =
                    info.getClass().getField(
                            "handler"
                    );

            return handler.get(info);
        } catch (
                ClassNotFoundException |
                NoSuchFieldException |
                NoSuchMethodException |
                IllegalAccessException |
                InvocationTargetException e
        ) {
            throw new IllegalStateException(
                    "Unable to resolve current Fabric fluid handler",
                    e
            );
        }
    }

    private static boolean isDefaultFabricHandler(Object handler) {
        return handler
                .getClass()
                .getName()
                .startsWith("net.fabricmc.fabric.");
    }
}
