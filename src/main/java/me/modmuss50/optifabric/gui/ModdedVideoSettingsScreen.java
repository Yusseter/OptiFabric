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
import me.modmuss50.optifabric.mixin.EntryListWidgetAccessor;

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
        /*
         * OptiFine draws submenu titles at y = 15.
         * A 39px vanilla header centers the 9px title there.
         */
        this.layout.setHeaderHeight(39);

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
                this.height / 6 + 179;

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
        /*
         * The section header occupies 13px before the
         * first option entry. Entry and widget content
         * offsets add another 4px, placing the first
         * option at OptiFine's exact height / 6 - 12.
         */
        int bodyY =
                this.height / 6 - 29;

        int bodyHeight =
                Math.max(
                        40,
                        doneY - bodyY - 3
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

            /*
             * Vanilla OptionListWidget uses 25px rows.
             * OptiFine submenus use a 21px row step.
             * Set this before any option entries are added.
             */
            ((EntryListWidgetAccessor) (Object) this)
                    .optifabric$setItemHeight(21);
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