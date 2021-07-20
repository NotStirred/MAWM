package io.github.notstirred.mawm.converter.task.source;

import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.converter.task.TaskRequest;
import net.minecraft.entity.player.EntityPlayer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BackupTaskSource implements TaskRequest.TaskSource {
    private final EntityPlayer player;

    public BackupTaskSource(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public Path getPath() {
        int head = MAWM.INSTANCE.getPlayerTaskHistory().get(player.getUniqueID()).getHead() + 1;
        return Paths.get(MAWM.INSTANCE.backupDirectory.toString() + "/" + player.getUniqueID() + "/" + head);
    }
}
