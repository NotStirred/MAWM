package io.github.notstirred.mawm.asm.mixininterfaces;

public interface IFreezableCube {

    boolean isCubeReadFrozen();
    boolean isCubeWriteFrozen();

    void freeze();
    void unFreeze();

}
