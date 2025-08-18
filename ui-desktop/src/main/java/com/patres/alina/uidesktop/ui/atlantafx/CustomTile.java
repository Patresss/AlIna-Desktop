package com.patres.alina.uidesktop.ui.atlantafx;

import atlantafx.base.controls.Tile;
import javafx.beans.NamedArg;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Workaround: https://github.com/mkpaz/atlantafx/issues/74
 * TileSkinBase title ChangeListener code is wrong #74
 */
public class CustomTile extends Tile {

    private final String formId;
    private final Supplier<String> valueSupplier;
    private final Consumer<String> valueConsumer;
    private final boolean mandatory;

    public CustomTile() {
        super(null, null, null);
        this.formId = null;
        this.valueSupplier = () -> null;
        this.valueConsumer = (value) -> {
        };
        this.mandatory = false;
    }


    public CustomTile(String id,
                      @Nullable @NamedArg("title") String title,
                      @Nullable @NamedArg("description") String description) {
        super(title, description, null);
        this.formId = id;
        this.valueSupplier = () -> null;
        this.valueConsumer = (value) -> {
        };
        this.mandatory = false;

    }

    public CustomTile(@Nullable @NamedArg("title") String title,
                      @Nullable @NamedArg("description") String description) {
        this(null, title, description);
    }

    public CustomTile(@Nullable String title,
                      @Nullable String description,
                      @Nullable Node graphic) {
        super(title, description, graphic);
        this.formId = null;
        this.valueSupplier = () -> null;
        this.valueConsumer = (value) -> {
        };
        this.mandatory = false;
    }

    public CustomTile(String id,
                      @Nullable String title,
                      @Nullable String description,
                      Supplier<String> valueSupplier,
                      Consumer<String> valueConsumer,
                      boolean mandatory) {
        super(title, description, null);
        this.formId = id;
        this.valueSupplier = valueSupplier;
        this.valueConsumer = valueConsumer;
        this.mandatory = mandatory;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new CustomTileSkin(this);
    }

    public String getFormId() {
        return formId;
    }

    public String getFormValue() {
        return valueSupplier.get();
    }

    public void setFormValue(String formValue) {
        valueConsumer.accept(formValue);
    }

    public boolean isMandatory() {
        return mandatory;
    }
}
