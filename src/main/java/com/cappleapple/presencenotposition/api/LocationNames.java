package com.cappleapple.presencenotposition.api;

import com.cappleapple.presencenotposition.location.LocationContext;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public final class LocationNames {
    private LocationNames() {
    }

    public static String translationKey(LocationContext context) {
        String prefix = switch (context.type()) {
            case STRUCTURE -> "structure";
            case BIOME -> "biome";
            case DIMENSION -> "dimension";
            case CUSTOM -> "presencenotposition.custom";
        };
        return prefix + "." + context.id().getNamespace() + "." + context.id().getPath().replace('/', '.');
    }

    public static Component defaultComponent(LocationContext context) {
        return Component.translatableWithFallback(translationKey(context), parsedPath(context.id().getPath()));
    }

    public static String parsedPath(String path) {
        String normalized = path.replace('/', ' ').replace('_', ' ');
        StringBuilder result = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            if (Character.isWhitespace(codePoint)) {
                result.appendCodePoint(codePoint);
                capitalize = true;
            } else if (capitalize) {
                result.appendCodePoint(Character.toTitleCase(codePoint));
                capitalize = false;
            } else {
                result.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }
}
