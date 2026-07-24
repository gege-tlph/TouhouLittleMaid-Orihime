package com.github.tartaricacid.touhoulittlemaid.network.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientAltarRecipeCacheSourceContractTest {
    private static final Path SOURCE = Path.of("..", "..", "src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "network", "client", "ClientAltarRecipeCache.java");

    @Test
    void altarSyncRefreshesOnlyTheTlmCreativeTab() throws IOException {
        String source = Files.readString(SOURCE);

        assertFalse(source.contains("CreativeModeTabs.tryRebuildTabContents"),
                "altar sync must not rebuild every mod's creative tab before join data is stable");
        assertTrue(source.contains("InitCreativeTabs.MAIN_TAB.buildContents"),
                "the synchronized entity placeholders still need to refresh TLM's own tab");
    }
}
