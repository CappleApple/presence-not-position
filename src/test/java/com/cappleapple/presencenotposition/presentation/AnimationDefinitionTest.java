package com.cappleapple.presencenotposition.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AnimationDefinitionTest {
    @Test void loopingAnimationWraps() {
        AnimationDefinition animation = new AnimationDefinition(3, 2, true, AnimationDefinition.Layout.VERTICAL);
        assertEquals(0, animation.frameAt(0));
        assertEquals(2, animation.frameAt(5));
        assertEquals(0, animation.frameAt(6));
    }

    @Test void nonLoopingAnimationHoldsFinalFrame() {
        AnimationDefinition animation = new AnimationDefinition(3, 2, false, AnimationDefinition.Layout.VERTICAL);
        assertEquals(2, animation.frameAt(100));
    }
}
