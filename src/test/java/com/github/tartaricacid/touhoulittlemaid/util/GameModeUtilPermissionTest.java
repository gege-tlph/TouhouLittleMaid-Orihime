package com.github.tartaricacid.touhoulittlemaid.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameModeUtilPermissionTest {
    @BeforeAll
    static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void dedicatedPermissionIsReevaluatedSoAnOpDowngradeRevokesSaving() {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        MinecraftServer server = mock(MinecraftServer.class);
        when(player.level()).thenReturn(level);
        when(level.getServer()).thenReturn(server);
        when(server.isSingleplayer()).thenReturn(false);
        when(server.isDedicatedServer()).thenReturn(true);
        when(player.permissions()).thenReturn(
                PermissionSet.ALL_PERMISSIONS, PermissionSet.NO_PERMISSIONS);

        assertTrue(GameModeUtil.canEditSite(player));
        assertFalse(GameModeUtil.canEditSite(player),
                "permission must be checked when the queued save executes, not when the page opens");
    }

    @Test
    void absentPlayerNeverHasConfigurationAuthority() {
        assertFalse(GameModeUtil.canEditSite(null));
    }
}
