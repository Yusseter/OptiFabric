package me.modmuss50.optifabric.mixin;

import net.minecraft.client.gui.widget.EntryListWidget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntryListWidget.class)
public interface EntryListWidgetAccessor {

    @Mutable
    @Accessor("itemHeight")
    void optifabric$setItemHeight(int itemHeight);
}