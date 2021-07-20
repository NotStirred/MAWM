package io.github.notstirred.mawm.converter;

import cubicchunks.converter.lib.convert.LevelInfoConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.convert.data.DualSourceCubicChunksColumnData;

public class NoopCC2CCDualSourceMergingLevelInfoConverter implements LevelInfoConverter<DualSourceCubicChunksColumnData, CubicChunksColumnData> {
    @Override public void convert() { }
}