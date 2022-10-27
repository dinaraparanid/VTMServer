package com.dinaraparanid.converter;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public enum TrackFileExtension {
    MP3 {
        @Override
        public String getExtension() {
            return "mp3";
        }
    },

    MP4 {
        @Override
        public String getExtension() {
            return "mp4";
        }
    },

    WAV {
        @Override
        public String getExtension() {
            return "wav";
        }
    },

    AAC {
        @Override
        public String getExtension() {
            return "aac";
        }
    },

    FLAC {
        @Override
        public String getExtension() {
            return "flac";
        }
    },

    M4A {
        @Override
        public String getExtension() {
            return "m4a";
        }
    },

    OPUS {
        @Override
        public String getExtension() {
            return "opus";
        }
    },

    VORBIS {
        @Override
        public String getExtension() {
            return "vorbis";
        }
    };

    @NotNull
    private static final Map<String, TrackFileExtension> extensionMap = TrackFileExtensionExtKt.getExtensionMap();

    @NotNull
    public static TrackFileExtension fromString(@NotNull String extension) {
        return extensionMap.get(extension);
    }

    @NotNull
    public abstract String getExtension();

    @Override
    public String toString() {
        return getExtension();
    }
}
