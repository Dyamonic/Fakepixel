package club.abelix.api.server.game.manager;

import club.abelix.network.common.logging.Logging;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
public class SchematicManager {

    private final File schematicFile;

    private final Location center;

    private CuboidRegion lobbyRegion;

    @Getter
    private EditSession session;

    private static EditSession newEditSession(String world) {
        return new EditSessionBuilder(world)
                .limitUnlimited()
                .fastmode(true)
                .build();
    }

    public void pasteSchematic(@NonNull Extent extent) {
        if (!schematicFile.exists()) {
            return;
        }

        try {
            Schematic schematic = FaweAPI.load(schematicFile);

            Vector vector = new Vector(center.getX(), center.getY(), center.getZ());

            Vector origin = schematic.getClipboard().getOrigin();
            Region region = schematic.getClipboard().getRegion();

            lobbyRegion = new CuboidRegion(session.getWorld(),
                    vector.add(region.getMinimumPoint().subtract(origin)),
                    vector.add(region.getMaximumPoint().subtract(origin))
            );

            schematic.paste(extent, vector, false);

            session.flushQueue();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetLobby() {
        if (session == null) {
            Logging.IMP.error("[Schematic] Session is null!");
            return;
        }

        try {
            session.setBlocks(lobbyRegion, new BaseBlock(BlockID.AIR));

            session.flushQueue();

            lobbyRegion = null;
            session = null;
        } catch (Exception any) {
            any.printStackTrace();
        }
    }

    public void resetSession() {
        session = newEditSession(center.getWorld().getName());
    }

}