package me.modmuss50.optifabric.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;

import me.modmuss50.optifabric.compatibility.VideoOptionsScreenCompat;

public final class ModdedVideoSettingsScreen
        extends GameOptionsScreen {

    private final VideoOptionsScreen videoOptionsScreen;
    private ButtonWidget doneButton;

    public ModdedVideoSettingsScreen(
            VideoOptionsScreen videoOptionsScreen,
            GameOptions gameOptions
    ) {
        super(
                videoOptionsScreen,
                gameOptions,
                Text.translatable(
                        "optifabric.video.modded.title"
                )
        );

        this.videoOptionsScreen =
                videoOptionsScreen;
    }

    @Override
    protected void init() {
        super.init();

        if (this.body == null || this.doneButton == null) {
            return;
        }

        /*
         * Match the vertical placement used by OptiFine's
         * Video Settings sub-screens instead of pinning Done
         * to the bottom of the modern vanilla layout.
         */
        int doneY =
                this.height / 6 + 168;

        this.doneButton.setX(
                this.width / 2 - 100
        );
        this.doneButton.setY(doneY);
        this.doneButton.setWidth(200);

        /*
         * Keep the vanilla OptionListWidget compatibility surface
         * for Fabric mods, but confine it to the same general area
         * occupied by OptiFine submenu options.
         */
        int bodyY = 32;

        int bodyHeight =
                Math.max(
                        40,
                        doneY - bodyY - 8
                );

        this.body.position(
                this.width,
                bodyHeight,
                0,
                bodyY
        );
    }

    @Override
    protected void initBody() {
        this.body =
                this.layout.addBody(
                        new OptiFineOptionListWidget(
                                this.client,
                                this.width,
                                this
                        )
                );

        this.addOptions();
    }

    @Override
    protected void initFooter() {
        this.doneButton =
                ButtonWidget.builder(
                        Text.translatable("gui.done"),
                        button -> optifabric$leave(
                                this.videoOptionsScreen
                        )
                )
                .dimensions(
                        0,
                        0,
                        200,
                        20
                )
                .build();

        this.layout.addFooter(
                this.doneButton
        );
    }

    @Override
    protected void addOptions() {
        ((VideoOptionsScreenCompat)
                (Object) this.videoOptionsScreen)
                .optifabric$collectModdedOptions(
                        this.body
                );
    }

    /*
     * OptiFine submenu behaviour:
     *
     * Done -> previous Video Settings screen
     * ESC  -> close the settings stack
     */
    @Override
    public void close() {
        optifabric$leave(null);
    }

    private void optifabric$leave(
            Screen nextScreen
    ) {
        if (this.body != null) {
            this.body.applyAllPendingValues();
        }

        this.gameOptions.write();

        if (this.client != null) {
            this.client.setScreen(nextScreen);
        }
    }

    /*
     * OptionListWidget is retained because Fabric mods contribute
     * their Video Settings options through this vanilla surface.
     *
     * Its modern vanilla list background/separator decorations are
     * suppressed so the result follows OptiFine's submenu styling.
     */
    private static final class OptiFineOptionListWidget
            extends OptionListWidget {

        private OptiFineOptionListWidget(
                MinecraftClient client,
                int width,
                GameOptionsScreen screen
        ) {
            super(client, width, screen);
        }

        @Override
        protected void drawHeaderAndFooterSeparators(
                DrawContext context
        ) {
        }

        @Override
        protected void drawMenuListBackground(
                DrawContext context
        ) {
        }
    }
}