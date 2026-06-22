package me.modmuss50.optifabric.patcher.fixes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Called from {@link FluidRendererFix}, here to avoid class loading Minecraft stuff too early */
public class FluidRendererFixExternal {
	public static boolean needsOptiFine(Object state) {
		Object fluid = getFluid(state);
		return isWater(fluid) && isFabricHandler(getRenderHandler(fluid));
	}

	private static Object getFluid(Object state) {
		Object resolved = invokeOptional(state, "getFluid");
		if (resolved != null) {
			return resolved;
		}

		resolved = invokeOptional(state, "getFluidState");
		if (resolved != null) {
			Object nested = invokeOptional(resolved, "getFluid");
			if (nested != null) {
				return nested;
			}

			return resolved;
		}

		throw new IllegalStateException("Unable to read fluid from " + state.getClass().getName());
	}

	private static Object invokeOptional(Object target, String methodName) {
		try {
			Method method = target.getClass().getMethod(methodName);
			return method.invoke(target);
		} catch (NoSuchMethodException e) {
			return null;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to invoke " + methodName + " on " + target.getClass().getName(), e);
		}
	}

	private static boolean isWater(Object fluid) {
		try {
			Class<?> fluids = Class.forName("net.minecraft.fluid.Fluids");
			Field water = fluids.getField("WATER");
			Field flowingWater = fluids.getField("FLOWING_WATER");
			return fluid == water.get(null) || fluid == flowingWater.get(null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to resolve water fluids", e);
		}
	}

	private static Object getRenderHandler(Object fluid) {
		try {
			Class<?> registryClass = Class.forName("net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry");
			Field instanceField = registryClass.getField("INSTANCE");
			Object registry = instanceField.get(null);
			Method getMethod = registryClass.getMethod("get", Class.forName("net.minecraft.fluid.Fluid"));
			return getMethod.invoke(registry, fluid);
		} catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException("Unable to resolve render handler", e);
		}
	}

	private static boolean isFabricHandler(Object handler) {
		return handler != null && handler.getClass().getName().startsWith("net.fabricmc.fabric.");
	}
}
