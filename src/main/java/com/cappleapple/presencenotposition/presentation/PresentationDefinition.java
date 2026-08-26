package com.cappleapple.presencenotposition.presentation;

import com.cappleapple.presencenotposition.music.MusicDefinition;
import javax.annotation.Nullable;

public record PresentationDefinition(
    TitleDefinition title,
    @Nullable EntrySoundDefinition entrySound,
    @Nullable MusicDefinition music
) { }
