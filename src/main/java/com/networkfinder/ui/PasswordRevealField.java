package com.networkfinder.ui;

import java.util.function.BiConsumer;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

/**
 * Password input with an eye toggle that reveals the current text. Bundles the
 * masked field, a plain-text mirror, a toggle button, and their synchronization
 * so every password field in the app shares one implementation.
 */
public class PasswordRevealField extends StackPane {

    private final PasswordField maskedField = new PasswordField();
    private final TextField plainField = new TextField();
    private final Button eyeButton = new Button();

    @SuppressWarnings("this-escape")

    public PasswordRevealField(String promptText) {
        maskedField.setPromptText(promptText);
        plainField.setPromptText(promptText);
        plainField.setVisible(false);
        plainField.setManaged(false);

        // Reserve room on the right so long passwords never run under the eye button.
        maskedField.setStyle("-fx-padding: 10px 34px 10px 14px;");
        plainField.setStyle("-fx-padding: 10px 34px 10px 14px;");

        eyeButton.setAccessibleText("Show password");
        eyeButton.setGraphic(SvgIcons.icon(SvgIcons.Glyph.EYE, 15));
        eyeButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        eyeButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;"
                + " -fx-padding: 0 8 0 0; -fx-focus-traversable: false;");
        StackPane.setAlignment(eyeButton, Pos.CENTER_RIGHT);

        eyeButton.setOnAction(e -> {
            boolean show = maskedField.isVisible();
            plainField.setText(maskedField.getText());
            maskedField.setVisible(!show);
            maskedField.setManaged(!show);
            plainField.setVisible(show);
            plainField.setManaged(show);
            eyeButton.setAccessibleText(show ? "Hide password" : "Show password");
            eyeButton.setGraphic(SvgIcons.icon(show ? SvgIcons.Glyph.EYE_OFF : SvgIcons.Glyph.EYE, 15));
            (show ? plainField : maskedField).requestFocus();
        });

        maskedField.textProperty().addListener((obs, old, val) -> {
            if (maskedField.isVisible()) {
                plainField.setText(val);
            }
        });
        plainField.textProperty().addListener((obs, old, val) -> {
            if (plainField.isVisible()) {
                maskedField.setText(val);
            }
        });

        getChildren().addAll(maskedField, plainField, eyeButton);
    }

    /** Current password content regardless of the reveal state. */
    public String getValue() {
        return maskedField.getText();
    }

    /** Sets the content of both the masked and mirrored fields. */
    public void setValue(String value) {
        maskedField.setText(value);
        plainField.setText(value);
    }

    /** Focuses whichever field is currently visible (for initial focus helpers). */
    public void requestValueFocus() {
        (maskedField.isVisible() ? maskedField : plainField).requestFocus();
    }

    /**
     * Attaches a listener fired with (oldValue, newValue) while the user types,
     * regardless of the reveal state.
     */
    public void onTextChanged(BiConsumer<String, String> listener) {
        maskedField.textProperty().addListener((obs, old, val) -> listener.accept(old, val));
        plainField.textProperty().addListener((obs, old, val) -> listener.accept(old, val));
    }

    /** Clears both masked and mirrored text. */
    public void clear() {
        maskedField.clear();
        plainField.clear();
    }
}
