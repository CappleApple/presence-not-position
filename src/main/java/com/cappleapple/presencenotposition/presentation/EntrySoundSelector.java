package com.cappleapple.presencenotposition.presentation;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/** Selects a single entry sting from the same top-to-bottom order used by the title stack. */
public final class EntrySoundSelector {
    private EntrySoundSelector() {
    }

    public static Optional<EntrySoundDefinition> select(List<Candidate> topToBottom) {
        for (Candidate candidate : topToBottom) {
            EntrySoundDefinition definition = candidate.definition();
            var override = candidate.override().sound();
            if (definition == null && override == null) continue;
            return Optional.of(new EntrySoundDefinition(
                override != null ? override : definition.id(),
                definition == null ? 1.0F : definition.volume(),
                definition == null ? 1.0F : definition.pitch()
            ));
        }
        return Optional.empty();
    }

    public record Candidate(@Nullable EntrySoundDefinition definition, PresentationOverride override) { }
}
