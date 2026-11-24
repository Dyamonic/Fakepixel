package club.abelix.games.fakevival.setup;

import com.google.gson.JsonObject;
import java.util.Map;

public record SMPSetupSnapshot(String snapshotId,String snapshotName,int formatVersion,String createdBy,long createdAt,JsonObject data,String dataHash,Map<String,Integer> sectionCounts) {}
