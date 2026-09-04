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
    private static volatile Method currentInfoMethod;
    private static volatile Field currentHandlerField;
    private static volatile Field naturalWatersAlphaField;
    private static volatile boolean naturalWatersAlphaFieldResolved;

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

    public static float getNaturalWatersAlpha(
            Object renderer,
            float original
    ) {
        Field field =
                getNaturalWatersAlphaField(
                        renderer.getClass()
                );

        if (field == null) {
            return original;
        }

        try {
            Object value =
                    field.get(renderer);

            if (value == null) {
                return original;
            }

            if (!(value instanceof ThreadLocal)) {
                throw new IllegalStateException(
                        "Natural Waters vertex alpha field " +
                                "is not a ThreadLocal"
                );
            }

            Object alpha =
                    ((ThreadLocal<?>) value).get();

            if (alpha == null) {
                return original;
            }

            if (!(alpha instanceof Number)) {
                throw new IllegalStateException(
                        "Natural Waters vertex alpha " +
                                "is not numeric"
                );
            }

            return ((Number) alpha).floatValue();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Unable to read Natural Waters vertex alpha",
                    e
            );
        }
    }

    private static Field getNaturalWatersAlphaField(
            Class<?> rendererClass
    ) {
        if (naturalWatersAlphaFieldResolved) {
            return naturalWatersAlphaField;
        }

        synchronized (FluidRendererFixExternal.class) {
            if (naturalWatersAlphaFieldResolved) {
                return naturalWatersAlphaField;
            }

            try {
                Field field =
                        rendererClass.getDeclaredField(
                                "naturalwaters$vertexAlpha"
                        );

                if (!ThreadLocal.class.isAssignableFrom(
                        field.getType()
                )) {
                    throw new IllegalStateException(
                            "Natural Waters vertex alpha field " +
                                    "has unexpected type: " +
                                    field.getType().getName()
                    );
                }

                field.setAccessible(true);

                naturalWatersAlphaField = field;
            } catch (NoSuchFieldException e) {
                naturalWatersAlphaField = null;
            }

            naturalWatersAlphaFieldResolved = true;

            return naturalWatersAlphaField;
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

    private static Method getCurrentInfoMethod() {
        Method method = currentInfoMethod;

        if (method != null) {
            return method;
        }

        synchronized (FluidRendererFixExternal.class) {
            method = currentInfoMethod;

            if (method != null) {
                return method;
            }

            try {
                Class<?> renderingImpl =
                        Class.forName(
                                "net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderingImpl"
                        );

                method =
                        renderingImpl.getMethod(
                                "getCurrentInfo"
                        );

                currentInfoMethod = method;
                return method;
            } catch (
                    ClassNotFoundException |
                    NoSuchMethodException e
            ) {
                throw new IllegalStateException(
                        "Unable to resolve Fabric fluid rendering info accessor",
                        e
                );
            }
        }
    }

    private static Field getCurrentHandlerField(
            Class<?> infoClass
    ) {
        Field field = currentHandlerField;

        if (field != null) {
            return field;
        }

        synchronized (FluidRendererFixExternal.class) {
            field = currentHandlerField;

            if (field != null) {
                return field;
            }

            try {
                field =
                        infoClass.getField(
                                "handler"
                        );

                currentHandlerField = field;
                return field;
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(
                        "Unable to resolve current Fabric fluid handler field",
                        e
                );
            }
        }
    }

    private static Object getCurrentHandler() {
        try {
            Object info =
                    getCurrentInfoMethod().invoke(null);

            if (info == null) {
                return null;
            }

            return getCurrentHandlerField(
                    info.getClass()
            ).get(info);
        } catch (
                IllegalAccessException |
                InvocationTargetException e
        ) {
            throw new IllegalStateException(
                    "Unable to read current Fabric fluid handler",
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
