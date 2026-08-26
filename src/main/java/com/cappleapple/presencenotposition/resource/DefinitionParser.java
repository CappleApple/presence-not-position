package com.cappleapple.presencenotposition.resource;

import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.music.MusicDefinition;
import com.cappleapple.presencenotposition.music.MusicSelection;
import com.cappleapple.presencenotposition.music.TrackDelay;
import com.cappleapple.presencenotposition.presentation.AnimationDefinition;
import com.cappleapple.presencenotposition.presentation.EntrySoundDefinition;
import com.cappleapple.presencenotposition.presentation.PresentationDefinition;
import com.cappleapple.presencenotposition.presentation.TitleDefinition;
import com.cappleapple.presencenotposition.presentation.VisualDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public final class DefinitionParser {
    private DefinitionParser() {
    }

    public static PresentationDefinition parse(LocationType type, JsonObject root) {
        TitleDefinition title = parseTitle(type, object(root, "title"));
        EntrySoundDefinition sound = root.has("entrySound") ? parseSound(GsonHelper.getAsJsonObject(root, "entrySound")) : null;
        MusicDefinition music = root.has("music") ? parseMusic(GsonHelper.getAsJsonObject(root, "music")) : null;
        return new PresentationDefinition(title, sound, music);
    }

    private static TitleDefinition parseTitle(LocationType type, @Nullable JsonObject json) {
        if (json == null) {
            return new TitleDefinition(VisualDefinition.text(), null, null, null, null, 80, 10, 20, type.defaultTitlePriority());
        }
        VisualDefinition visual = json.has("visual") ? parseVisual(GsonHelper.getAsJsonObject(json, "visual")) : VisualDefinition.text();
        return new TitleDefinition(
            visual,
            string(json, "text"),
            string(json, "translationKey"),
            string(json, "subtitle"),
            string(json, "subtitleTranslationKey"),
            GsonHelper.getAsInt(json, "duration", 80),
            GsonHelper.getAsInt(json, "fadeIn", 10),
            GsonHelper.getAsInt(json, "fadeOut", 20),
            GsonHelper.getAsInt(json, "priority", type.defaultTitlePriority())
        );
    }

    private static VisualDefinition parseVisual(JsonObject json) {
        VisualDefinition.Type type = VisualDefinition.Type.valueOf(GsonHelper.getAsString(json, "type", "text").toUpperCase(Locale.ROOT));
        ResourceLocation texture = json.has("texture") ? id(GsonHelper.getAsString(json, "texture")) : null;
        List<ResourceLocation> frames = new ArrayList<>();
        if (json.has("frames")) {
            JsonArray array = GsonHelper.getAsJsonArray(json, "frames");
            for (JsonElement element : array) frames.add(id(element.getAsString()));
        }
        JsonObject animationJson = object(json, "animation");
        int frameCount = animationJson == null ? Math.max(1, frames.size()) : GsonHelper.getAsInt(animationJson, "frameCount", Math.max(1, frames.size()));
        int frameTime = animationJson == null ? 1 : GsonHelper.getAsInt(animationJson, "frameTime", 1);
        boolean loop = animationJson != null && GsonHelper.getAsBoolean(animationJson, "loop", false);
        AnimationDefinition.Layout layout = animationJson == null
            ? AnimationDefinition.Layout.AUTO
            : AnimationDefinition.Layout.valueOf(GsonHelper.getAsString(animationJson, "layout", "auto").toUpperCase(Locale.ROOT));
        return new VisualDefinition(type, texture, frames, new AnimationDefinition(frameCount, frameTime, loop, layout));
    }

    private static EntrySoundDefinition parseSound(JsonObject json) {
        return new EntrySoundDefinition(
            id(GsonHelper.getAsString(json, "id")),
            GsonHelper.getAsFloat(json, "volume", 1.0F),
            GsonHelper.getAsFloat(json, "pitch", 1.0F)
        );
    }

    private static MusicDefinition parseMusic(JsonObject json) {
        return new MusicDefinition(
            resource(json, "folder"),
            resource(json, "dayFolder"),
            resource(json, "nightFolder"),
            GsonHelper.getAsFloat(json, "volume", 1.0F),
            GsonHelper.getAsBoolean(json, "normalizeVolume", false),
            GsonHelper.getAsDouble(json, "normalizationTarget", -16.0),
            MusicSelection.valueOf(GsonHelper.getAsString(json, "selection", "shuffle").toUpperCase(Locale.ROOT)),
            GsonHelper.getAsBoolean(json, "avoidImmediateRepeat", true),
            GsonHelper.getAsBoolean(json, "startAfterTitle", false),
            GsonHelper.getAsDouble(json, "startDelay", 0.0),
            parseDelay(json.get("trackDelay")),
            GsonHelper.getAsDouble(json, "fadeIn", 4.0),
            GsonHelper.getAsDouble(json, "fadeOut", 5.0),
            GsonHelper.getAsDouble(json, "transitionFadeIn", GsonHelper.getAsDouble(json, "fadeIn", 4.0)),
            GsonHelper.getAsDouble(json, "transitionFadeOut", GsonHelper.getAsDouble(json, "fadeOut", 5.0)),
            json.has("transitionDelay") ? GsonHelper.getAsDouble(json, "transitionDelay") : null,
            GsonHelper.getAsBoolean(json, "resume", true),
            GsonHelper.getAsBoolean(json, "silenceLowerPriority", false),
            GsonHelper.getAsInt(json, "priority", 0)
        );
    }

    private static TrackDelay parseDelay(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) return new TrackDelay(20.0, 60.0);
        if (element.isJsonPrimitive()) {
            double fixed = element.getAsDouble();
            return new TrackDelay(fixed, fixed);
        }
        JsonObject object = element.getAsJsonObject();
        return new TrackDelay(GsonHelper.getAsDouble(object, "min", 0.0), GsonHelper.getAsDouble(object, "max", 0.0));
    }

    @Nullable
    private static JsonObject object(JsonObject parent, String key) {
        return parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null;
    }

    @Nullable
    private static String string(JsonObject parent, String key) {
        return parent.has(key) && !parent.get(key).isJsonNull() ? parent.get(key).getAsString() : null;
    }

    @Nullable
    private static ResourceLocation resource(JsonObject parent, String key) {
        return parent.has(key) ? id(GsonHelper.getAsString(parent, key)) : null;
    }

    private static ResourceLocation id(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) throw new IllegalArgumentException("Invalid resource location: " + value);
        return id;
    }
}
