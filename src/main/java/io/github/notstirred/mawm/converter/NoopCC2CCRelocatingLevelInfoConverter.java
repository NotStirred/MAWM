package io.github.notstirred.mawm.converter;

import cubicchunks.converter.lib.convert.LevelInfoConverter;
import cubicchunks.converter.lib.convert.data.PriorityCubicChunksColumnData;

public class NoopCC2CCRelocatingLevelInfoConverter implements LevelInfoConverter<PriorityCubicChunksColumnData, PriorityCubicChunksColumnData> {
    @Override public void convert() { }
}
