package me.modmuss50.optifabric.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;

import me.modmuss50.optifabric.compatibility.VideoOptionsScreenCompat;
import me.modmuss50.optifabric.gui.ModdedVideoSettingsScreen;

@Mixin(VideoOptionsScreen.class)
abstract class VideoOptionsScreenMixin
        extends GameOptionsScreen
        implements VideoOptionsScreenCompat {

    private static boolean optifabric$contributionWarningPrinted;

    protected VideoOptionsScreenMixin(
            Screen parent,
            GameOptions gameOptions,
            Text title
    ) {
        super(parent, gameOptions, title);
    }

    @Override
    public void optifabric$collectModdedOptions(
            OptionListWidget target
    ) {
        OptionListWidget previous = this.body;

        this.body = target;

        try {
            this.addOptions();
        } finally {
            this.body = previous;
        }
    }

    @Inject(
            method = "init()V",
            at = @At("TAIL"),
            require = 0
    )
    private void optifabric$layoutCompatibilityButtons(
            CallbackInfo call
    ) {
        MinecraftClient client =
                MinecraftClient.getInstance();

        List<ClickableWidget> widgets =
                new ArrayList<ClickableWidget>();

        ClickableWidget doneButton = null;
        String doneText =
                Text.translatable("gui.done").getString();

        for (Element element : this.children()) {
            if (!(element instanceof ClickableWidget)) {
                continue;
            }

            ClickableWidget widget =
                    (ClickableWidget) element;

            widgets.add(widget);

            if (
                    doneText.equals(
                            widget.getMessage().getString()
                    )
            ) {
                doneButton = widget;
            }
        }

        if (doneButton == null) {
            return;
        }

        int originalDoneY = doneButton.getY();

        int lastRowY = Integer.MIN_VALUE;

        for (ClickableWidget widget : widgets) {
            if (
                    widget == doneButton
                            || widget.getY()
                                    >= originalDoneY
            ) {
                continue;
            }

            lastRowY =
                    Math.max(
                            lastRowY,
                            widget.getY()
                    );
        }

        if (lastRowY == Integer.MIN_VALUE) {
            return;
        }

        ClickableWidget rightButton = null;

        for (ClickableWidget widget : widgets) {
            if (widget.getY() != lastRowY) {
                continue;
            }

            if (
                    rightButton == null
                            || widget.getX()
                                    > rightButton.getX()
            ) {
                rightButton = widget;
            }
        }

        if (rightButton == null) {
            return;
        }

        int previousRowY = Integer.MIN_VALUE;

        for (ClickableWidget widget : widgets) {
            if (widget.getY() >= lastRowY) {
                continue;
            }

            previousRowY =
                    Math.max(
                            previousRowY,
                            widget.getY()
                    );
        }

        int normalRowStep =
                previousRowY == Integer.MIN_VALUE
                        ? 24
                        : Math.max(
                                21,
                                lastRowY - previousRowY
                        );

        /*
         * Keep OptiFine's original larger separation before Done,
         * while inserting Other as a normal settings row.
         */
        int doneGap = Math.max(
                normalRowStep,
                originalDoneY - lastRowY
        );

        int moddedX = rightButton.getX();
        int moddedY = rightButton.getY();
        int moddedWidth = rightButton.getWidth();

        int otherY =
                lastRowY + normalRowStep;

        int newDoneY =
                otherY + doneGap;

        rightButton.setX(
                moddedX - moddedWidth - 10
        );
        rightButton.setY(otherY);

        doneButton.setY(newDoneY);

        ButtonWidget moddedButton =
                ButtonWidget.builder(
                        Text.translatable(
                                "optifabric.video.modded"
                        ),
                        button -> client.setScreen(
                                new ModdedVideoSettingsScreen(
                                        (VideoOptionsScreen)
                                                (Object) this,
                                        client.options
                                )
                        )
                )
                .dimensions(
                        moddedX,
                        moddedY,
                        moddedWidth,
                        20
                )
                .build();

        moddedButton.active =
                optifabric$hasModdedVideoOptions(
                        client
                );

        this.addDrawableChild(moddedButton);
    }

    private boolean optifabric$hasModdedVideoOptions(
            MinecraftClient client
    ) {
        try {
            ModdedVideoSettingsScreen owner =
                    new ModdedVideoSettingsScreen(
                            (VideoOptionsScreen)
                                    (Object) this,
                            client.options
                    );

            OptionListWidget probe =
                    new OptionListWidget(
                            client,
                            this.width,
                            owner
                    );

            this.optifabric$collectModdedOptions(
                    probe
            );

            return ModdedVideoSettingsScreen
                    .optifabric$hasOptionEntries(
                            probe
                    );
        } catch (Exception e) {
            optifabric$warnContributionProbe(e);
            return false;
        } catch (LinkageError e) {
            optifabric$warnContributionProbe(e);
            return false;
        }
    }

    private static void optifabric$warnContributionProbe(
            Throwable error
    ) {
        if (optifabric$contributionWarningPrinted) {
            return;
        }

        optifabric$contributionWarningPrinted = true;

        System.err.println(
                "[OptiFabric] Failed to probe modded "
                        + "video settings"
        );

        error.printStackTrace();
    }

    @Dynamic("OptiFine-specific video options action method")
    @SuppressWarnings("target")
    @Inject(
            method = "actionPerformed(Lnet/optifine/gui/GuiButtonOF;I)V",
            at = @At(value = "NEW", target = "net/optifine/shaders/gui/GuiShaders"),
            cancellable = true,
            remap = false
    )
    private void actionPerformed(CallbackInfo call) {
        if (FabricLoader.getInstance().isModLoaded("satin")) {
            Config.callShowGuiMessage(
                    "Shaders are not compatible with the Satin mod",
                    "Please remove this mod to enable Shaders"
            );
            call.cancel();
        }
    }
}