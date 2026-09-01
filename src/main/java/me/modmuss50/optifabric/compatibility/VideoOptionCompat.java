package me.modmuss50.optifabric.compatibility;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;

public final class VideoOptionCompat {
    private static final String[][] METHOD_PAIRS = {
            {
                    "optifabric$getBaselineDisplayOptions",
                    "optifabric$getDisplayOptions"
            },
            {
                    "optifabric$getBaselineQualityOptions",
                    "optifabric$getQualityOptions"
            },
            {
                    "optifabric$getBaselineInterfaceOptions",
                    "optifabric$getInterfaceOptions"
            }
    };

    private static final Set<String> REPLACED_OPTION_KEYS =
            new HashSet<String>();

    private static boolean warningPrinted;

    private VideoOptionCompat() {
    }

    public static SimpleOption<?> getReplacement(
            SimpleOption<?> original,
            GameOptions options
    ) {
        for (String[] pair : METHOD_PAIRS) {
            SimpleOption<?>[] baseline =
                    invoke(pair[0], options);

            SimpleOption<?>[] modified =
                    invoke(pair[1], options);

            SimpleOption<?> replacement =
                    findReplacement(
                            original,
                            baseline,
                            modified
                    );

            if (replacement != null) {
                String key =
                        getResourceKey(original);

                if (key != null) {
                    REPLACED_OPTION_KEYS.add(key);
                }

                return replacement;
            }
        }

        return original;
    }

    public static boolean isReplacedOption(
            SimpleOption<?> option
    ) {
        String key =
                getResourceKey(option);

        return key != null
                && REPLACED_OPTION_KEYS.contains(key);
    }

    private static SimpleOption<?> findReplacement(
            SimpleOption<?> original,
            SimpleOption<?>[] baseline,
            SimpleOption<?>[] modified
    ) {
        for (int i = 0; i < baseline.length; i++) {
            if (baseline[i] != original) {
                continue;
            }

            /*
             * If the original still exists somewhere in the modified
             * sequence, this was an insertion or reorder rather than
             * a replacement.
             */
            if (containsIdentity(modified, original)) {
                return null;
            }

            if (i >= modified.length) {
                return null;
            }

            SimpleOption<?> candidate =
                    modified[i];

            /*
             * A candidate already present in the baseline sequence means
             * the arrays shifted because of a deletion/reorder. Do not
             * guess in that case.
             */
            if (containsIdentity(baseline, candidate)) {
                return null;
            }

            return candidate;
        }

        return null;
    }

    private static boolean containsIdentity(
            SimpleOption<?>[] array,
            SimpleOption<?> option
    ) {
        for (SimpleOption<?> entry : array) {
            if (entry == option) {
                return true;
            }
        }

        return false;
    }

    private static String getResourceKey(
            SimpleOption<?> option
    ) {
        try {
            Method method =
                    option.getClass().getMethod(
                            "getResourceKey"
                    );

            Object result =
                    method.invoke(option);

            return result instanceof String
                    ? (String) result
                    : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static SimpleOption<?>[] invoke(
            String methodName,
            GameOptions options
    ) {
        try {
            Method method =
                    VideoOptionsScreen.class.getDeclaredMethod(
                            methodName,
                            GameOptions.class
                    );

            method.setAccessible(true);

            return (SimpleOption<?>[]) method.invoke(
                    null,
                    options
            );
        } catch (ReflectiveOperationException e) {
            if (!warningPrinted) {
                warningPrinted = true;

                System.err.println(
                        "[OptiFabric] Failed to resolve modified "
                                + "vanilla video options"
                );

                e.printStackTrace();
            }

            return new SimpleOption<?>[0];
        }
    }
}