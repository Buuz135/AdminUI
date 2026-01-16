package com.buuz135.adminui.util;

import com.buuz135.adminui.AdminUI;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.auth.ProfileServiceClient;
import com.hypixel.hytale.server.core.auth.ServerAuthManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nullable;
import java.util.Collection;

public class AuthUtil {

    @Nullable
    public static ProfileServiceClient.PublicGameProfile getProfile(String playerName) {
        for(World world : Universe.get().getWorlds().values()) {
            Collection<PlayerRef> playerRefs = world.getPlayerRefs();
            PlayerRef playerRef = NameMatching.DEFAULT.find(playerRefs, playerName, PlayerRef::getUsername);
            if (playerRef != null) {
                return new ProfileServiceClient.PublicGameProfile(playerRef.getUuid(), playerRef.getUsername());
            }
        }

        ServerAuthManager authManager = ServerAuthManager.getInstance();
        String sessionToken = authManager.getSessionToken();
        if (sessionToken == null) {
            AdminUI.getInstance().getLogger().atWarning().log("No session token available for profile lookup");
            return null;
        } else {
            ProfileServiceClient profileClient = authManager.getProfileServiceClient();
            return profileClient.getProfileByUsername(playerName, sessionToken);
        }
    }
}
