package com.cappleapple.presencenotposition.presentation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/** Selects a single entry sting from the same top-to-bottom order used by the title stack. */
public final class EntrySoundSelector {
    private EntrySoundSelector() {
    }

    public static Optional<EntrySoundDefinition> select(List<Candidate> topToBottom) {
        return select(topToBottom, ignored -> true, ThreadLocalRandom.current());
    }

    public static Optional<EntrySoundDefinition> select(
        List<Candidate> topToBottom, Predicate<ResourceLocation> available, RandomGenerator random
    ) {
        for (Candidate candidate : topToBottom) {
            EntrySoundDefinition definition = candidate.definition();
            var override = candidate.override().sound();
            if (definition == null && override == null) continue;
            List<ResourceLocation> ids = (override == null ? definition.ids() : List.of(override)).stream()
                .filter(available).toList();
            if (ids.isEmpty()) continue;
            return Optional.of(new EntrySoundDefinition(
                ids.get(random.nextInt(ids.size())),
                definition == null ? 1.0F : definition.volume(),
                definition == null ? 1.0F : definition.pitch()
            ));
        }
        return Optional.empty();
    }

    public record Candidate(@Nullable EntrySoundDefinition definition, PresentationOverride override) { }
}
