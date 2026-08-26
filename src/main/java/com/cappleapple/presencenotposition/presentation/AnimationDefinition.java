package com.cappleapple.presencenotposition.presentation;

public record AnimationDefinition(int frameCount, int frameTimeTicks, boolean loop, Layout layout) {
    public enum Layout { AUTO, VERTICAL, HORIZONTAL }

    public AnimationDefinition {
        if (frameCount < 1) frameCount = 1;
        if (frameTimeTicks < 1) frameTimeTicks = 1;
    }

    public int frameAt(int elapsedTicks) {
        int raw = Math.max(0, elapsedTicks) / this.frameTimeTicks;
        return this.loop ? raw % this.frameCount : Math.min(this.frameCount - 1, raw);
    }
}
