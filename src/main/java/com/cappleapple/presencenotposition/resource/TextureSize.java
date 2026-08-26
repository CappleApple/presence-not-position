package com.cappleapple.presencenotposition.resource;

public record TextureSize(int width, int height) {
    public TextureSize {
        if (width < 1 || height < 1) throw new IllegalArgumentException("texture dimensions must be positive");
    }
}
