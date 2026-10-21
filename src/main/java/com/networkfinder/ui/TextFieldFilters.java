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

    /**
     * Student ID fields: live guard for the 00000000-A format (service rule in
     * {@link com.networkfinder.service.InputValidator}). Typed lowercase
     * letters are auto-uppercased, the dash is inserted automatically after
     * the 8th digit, and wrong characters are rejected as they are typed.
     */
    public static TextFormatter<String> studentIdFilter() {
        return new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }
            String typed = change.getText();
            if (!typed.isEmpty()) {
                String normalized = typed.toUpperCase();
                if (!normalized.matches("[0-9A-Z]+")) {
                    return null;
                }
                if (!normalized.equals(typed)) {
                    change.setText(normalized);
                }
            }
            String resulting = change.getControlNewText();
            if (resulting.length() > com.networkfinder.service.InputValidator.getMaxIdLength()) {
                return null;
            }
            if (!resulting.matches("[0-9]{0,8}(-[A-Z]{0,1})?")) {
                return null;
            }
            if (resulting.matches("[0-9]{8}")) {
                change.setRange(0, change.getControlText().length());
                change.setText(resulting + "-");
                change.setCaretPosition(9);
                change.setAnchor(9);
            } else if (resulting.matches("[0-9]{8}[A-Z]")) {
                change.setRange(0, change.getControlText().length());
                change.setText(resulting.substring(0, 8) + "-" + resulting.substring(8));
                change.setCaretPosition(10);
                change.setAnchor(10);
            }
            return change;
        });
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
