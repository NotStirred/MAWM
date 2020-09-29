package io.github.notstirred.mawm.asm.mixininterfaces;

public interface IFreezableColumn {

    boolean isColumnReadFrozen();
    boolean isColumnWriteFrozen();

    void freeze();
    void unFreeze();

}
