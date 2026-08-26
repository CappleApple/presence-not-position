package com.cappleapple.presencenotposition.location;

public record LocationTransition(boolean entered, LocationContext context) {
    public static LocationTransition enter(LocationContext context) {
        return new LocationTransition(true, context);
    }

    public static LocationTransition exit(LocationContext context) {
        return new LocationTransition(false, context);
    }
}
