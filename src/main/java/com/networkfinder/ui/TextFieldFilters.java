package com.networkfinder.ui;

import javafx.scene.control.TextFormatter;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Live text-field guards that stop invalid characters at the keyboard.
 * Shared by the registration form and the admin add/edit dialogs so the
 * typing experience matches the service validation rules exactly.
 */
public final class TextFieldFilters {

    private TextFieldFilters() {
    }

    /** Person name fields (first name, surname): letters and single internal spaces only. */
    public static TextFormatter<String> nameFilter(int maxLength) {
        return new TextFormatter<>(new FilterOperator(maxLength, change -> {
            String text = change.getText();
            if (text.isEmpty()) {
                return true;
            }
            // Allow plain letters and single spaces only.
            if (!text.matches("[A-Za-z ]+")) {
                return false;
            }
            // Never allow two spaces in a row or a leading space.
            String resulting = change.getControlNewText();
            return !resulting.contains("  ") && !resulting.startsWith(" ");
        }));
    }

    /** Short name fields (middle initial, suffix): at most two letters. */
    public static TextFormatter<String> shortNameFilter() {
        return new TextFormatter<>(new FilterOperator(2, change -> {
            String text = change.getText();
            return text.isEmpty() || text.matches("[A-Za-z]+");
        }));
    }

    /** Student ID fields: digits only, capped at maxLength. */
    public static TextFormatter<String> studentIdFilter(int maxLength) {
        return new TextFormatter<>(new FilterOperator(maxLength, change -> {
            String text = change.getText();
            return text.isEmpty() || text.matches("[0-9]+");
        }));
    }

    /**
     * Operator that applies a character-level acceptance test and a hard
     * length cap before the text lands in the field.
     */
    private static final class FilterOperator implements UnaryOperator<TextFormatter.Change> {
        private final int maxLength;
        private final Predicate<TextFormatter.Change> acceptRule;

        private FilterOperator(int maxLength, Predicate<TextFormatter.Change> acceptRule) {
            this.maxLength = maxLength;
            this.acceptRule = acceptRule;
        }

        @Override
        public TextFormatter.Change apply(TextFormatter.Change change) {
            if (!change.isContentChange()) {
                return acceptRule.test(change) ? change : null;
            }
            if (change.getControlNewText().length() > maxLength) {
                return null;
            }
            return acceptRule.test(change) ? change : null;
        }
    }
}
