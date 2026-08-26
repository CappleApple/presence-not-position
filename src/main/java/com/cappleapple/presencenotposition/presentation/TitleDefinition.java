package com.cappleapple.presencenotposition.presentation;

import javax.annotation.Nullable;

public record TitleDefinition(
    VisualDefinition visual,
    @Nullable String text,
    @Nullable String translationKey,
    @Nullable String subtitle,
    @Nullable String subtitleTranslationKey,
    int durationTicks,
    int fadeInTicks,
    int fadeOutTicks,
    int priority
) {
    public TitleDefinition {
        if (durationTicks < 1) durationTicks = 1;
        fadeInTicks = Math.max(0, fadeInTicks);
        fadeOutTicks = Math.max(0, fadeOutTicks);
    }
}
