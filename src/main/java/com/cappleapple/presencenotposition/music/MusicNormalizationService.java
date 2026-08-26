package com.cappleapple.presencenotposition.music;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads cached Vorbis loudness tags without decoding audio samples. */
public final class MusicNormalizationService {
    private static final int MAX_COMMENT_SCAN = 256 * 1024;
    private static final Pattern PNP_LUFS = Pattern.compile("PNP_LOUDNESS_LUFS=(-?[0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPLAY_GAIN = Pattern.compile("REPLAYGAIN_TRACK_GAIN=(-?[0-9]+(?:\\.[0-9]+)?)\\s*dB", Pattern.CASE_INSENSITIVE);
    private static final Pattern R128_GAIN = Pattern.compile("R128_TRACK_GAIN=(-?[0-9]+)", Pattern.CASE_INSENSITIVE);

    private MusicNormalizationService() {
    }

    public static NormalizationMetadata read(InputStream input) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (out.size() < MAX_COMMENT_SCAN) {
            int count = input.read(buffer, 0, Math.min(buffer.length, MAX_COMMENT_SCAN - out.size()));
            if (count < 0) break;
            out.write(buffer, 0, count);
        }
        return parse(new String(out.toByteArray(), StandardCharsets.ISO_8859_1));
    }

    public static NormalizationMetadata parse(String comments) {
        String normalized = comments.toUpperCase(Locale.ROOT).replace('\0', '\n');
        Matcher pnp = PNP_LUFS.matcher(normalized);
        if (pnp.find()) return new NormalizationMetadata(Double.parseDouble(pnp.group(1)), null);
        Matcher r128 = R128_GAIN.matcher(normalized);
        if (r128.find()) {
            double adjustment = Integer.parseInt(r128.group(1)) / 256.0;
            return new NormalizationMetadata(-23.0 - adjustment, null);
        }
        Matcher replayGain = REPLAY_GAIN.matcher(normalized);
        if (replayGain.find()) return new NormalizationMetadata(null, Double.parseDouble(replayGain.group(1)));
        return NormalizationMetadata.UNKNOWN;
    }
}
