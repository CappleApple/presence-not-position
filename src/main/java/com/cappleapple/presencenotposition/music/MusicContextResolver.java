package com.cappleapple.presencenotposition.music;

import com.cappleapple.presencenotposition.location.LocationContext;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class MusicContextResolver {
    private MusicContextResolver() {
    }

    public static Optional<MusicChoice> resolve(
        Collection<LocationContext> active,
        Map<LocationContext, ResolvedMusic> definitions,
        Predicate<LocationContext> categoryEnabled,
        DayPeriod period
    ) {
        return active.stream()
            .filter(categoryEnabled)
            .filter(definitions::containsKey)
            .filter(context -> definitions.get(context).isUsable(period))
            .sorted(Comparator
                .comparingInt((LocationContext context) -> context.type().musicPriority()).reversed()
                .thenComparing(Comparator.comparingInt((LocationContext context) -> definitions.get(context).definition().priority()).reversed())
                .thenComparing(context -> context.id().toString()))
            .findFirst()
            .map(context -> {
                ResolvedMusic resolved = definitions.get(context);
                return new MusicChoice(context, resolved.definition(), period, resolved.tracks().resolve(period));
            });
    }
}
