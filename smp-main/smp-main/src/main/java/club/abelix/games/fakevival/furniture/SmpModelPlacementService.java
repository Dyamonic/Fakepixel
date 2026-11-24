package club.abelix.games.fakevival.furniture;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public final class SmpModelPlacementService {
    public static final int MAX_MODEL_PARTS = 50;
    public static final int MAX_GROUPS_PER_CHUNK = 50;
    public static final int MAX_PARTS_PER_CHUNK = 500;

    private final NamespacedKey markerKey, groupKey, furnitureKey, ownerKey, protectedKey;

    public SmpModelPlacementService(JavaPlugin plugin) {
        markerKey=new NamespacedKey(plugin,"smp_furniture");groupKey=new NamespacedKey(plugin,"furniture_group_id");
        furnitureKey=new NamespacedKey(plugin,"furniture_id");ownerKey=new NamespacedKey(plugin,"furniture_owner_uuid");
        protectedKey=new NamespacedKey(plugin,"protected_spawn");
    }

    public Optional<SpawnSession> begin(SmpFurnitureModel model, Location origin, float yaw, UUID owner, boolean protectedArea, UUID requestedGroup) {
        if (origin.getWorld()==null || model.parts().isEmpty() || model.parts().size()>MAX_MODEL_PARTS) return Optional.empty();
        UUID group=requestedGroup==null?UUID.randomUUID():requestedGroup;
        Chunk chunk=origin.getChunk();
        Set<String> groups=new HashSet<>();int parts=0;
        for(Entity entity:chunk.getEntities()) {
            String id=entity.getPersistentDataContainer().get(groupKey,PersistentDataType.STRING);
            if(id!=null){groups.add(id);parts++;if(id.equals(group.toString()))return Optional.empty();}
        }
        int visibleParts=(int)model.parts().stream().filter(part->part.visible()||part.entityType()==SmpModelPart.EntityType.ARMOR_STAND).count()+1;
        if(groups.size()>=MAX_GROUPS_PER_CHUNK || parts+visibleParts>MAX_PARTS_PER_CHUNK)return Optional.empty();
        return Optional.of(new SpawnSession(model,origin.clone(),yaw,owner,protectedArea,group));
    }

    public int spawnNext(SpawnSession session,int budget) {
        int spawned=0;
        try {
            while(spawned<budget && session.index<session.model.parts().size()) {
                SmpModelPart part=session.model.parts().get(session.index++);
                if(!part.visible()&&part.entityType()!=SmpModelPart.EntityType.ARMOR_STAND)continue;
                Entity entity=spawnPart(session,part);session.entities.add(entity);spawned++;
            }
            if(spawned<budget && session.index>=session.model.parts().size() && !session.hitboxSpawned) {
                session.hitboxSpawned=true;
                if(session.model.hitboxWidth()>0&&session.model.hitboxHeight()>0) {
                    Interaction hitbox=session.origin.getWorld().spawn(session.origin,Interaction.class,entity->{
                        entity.setInteractionWidth((float)session.model.hitboxWidth());entity.setInteractionHeight((float)session.model.hitboxHeight());
                        entity.setResponsive(true);entity.setPersistent(true);tag(entity,session);
                    });
                    session.entities.add(hitbox);spawned++;
                }
            }
        } catch(Exception exception) {session.failed=true;session.entities.forEach(Entity::remove);}
        return spawned;
    }

    public boolean complete(SpawnSession session){return session.failed||session.index>=session.model.parts().size()&&session.hitboxSpawned;}
    public boolean failed(SpawnSession session){return session.failed;}
    public PlacedGroup result(SpawnSession session){return new PlacedGroup(session.group,List.copyOf(session.entities));}
    public void abort(SpawnSession session){session.failed=true;session.entities.forEach(Entity::remove);}

    private Entity spawnPart(SpawnSession session,SmpModelPart part) {
        Location at=offset(session.origin,part,session.yaw);
        return switch(part.entityType()) {
            case BLOCK_DISPLAY -> at.getWorld().spawn(at,BlockDisplay.class,display->{
                display.setBlock(Bukkit.createBlockData(part.material()));transform(display,part,session.yaw);configureDisplay(display);tag(display,session);
            });
            case ITEM_DISPLAY -> at.getWorld().spawn(at,ItemDisplay.class,display->{
                display.setItemStack(new ItemStack(part.material()));transform(display,part,session.yaw);configureDisplay(display);tag(display,session);
            });
            case ARMOR_STAND -> at.getWorld().spawn(at,ArmorStand.class,stand->{configureArmorStand(stand,part,session.yaw);tag(stand,session);});
        };
    }

    private void configureDisplay(Display display){display.setPersistent(true);display.setGlowing(false);display.setCustomNameVisible(false);display.setInterpolationDelay(0);display.setInterpolationDuration(0);display.setTeleportDuration(0);}
    private void configureArmorStand(ArmorStand stand,SmpModelPart part,float facing) {
        stand.setInvisible(!part.visible());stand.setSmall(part.small());stand.setMarker(part.marker());stand.setGravity(false);
        stand.setBasePlate(part.basePlate());stand.setArms(part.arms());stand.setCanMove(false);stand.setPersistent(true);stand.setInvulnerable(true);
        stand.setSilent(true);stand.setCollidable(false);stand.setGlowing(false);stand.setCustomNameVisible(false);
        if(stand.getEquipment()!=null)stand.getEquipment().setItem(part.slot(),new ItemStack(part.material()),true);
        stand.setRotation(FurnitureRotation.toBukkitYaw(facing),0);EulerAngle pose=new EulerAngle(Math.toRadians(part.pitch()),Math.toRadians(part.yaw()),Math.toRadians(part.roll()));
        if(part.slot()==org.bukkit.inventory.EquipmentSlot.HEAD)stand.setHeadPose(pose);else if(part.slot()==org.bukkit.inventory.EquipmentSlot.HAND)stand.setRightArmPose(pose);else if(part.slot()==org.bukkit.inventory.EquipmentSlot.OFF_HAND)stand.setLeftArmPose(pose);
    }
    private void transform(Display display,SmpModelPart part,float facing){float yaw=(float)-Math.toRadians(FurnitureRotation.normalize(facing+part.yaw())),pitch=(float)Math.toRadians(part.pitch()),roll=(float)Math.toRadians(part.roll());Quaternionf rotation=new Quaternionf().rotateY(yaw).rotateX(pitch).rotateZ(roll);display.setTransformation(new Transformation(new Vector3f(),rotation,new Vector3f(part.scaleX(),part.scaleY(),part.scaleZ()),new Quaternionf()));}
    private Location offset(Location origin,SmpModelPart part,float yaw){return origin.clone().add(FurnitureRotation.rotate(new org.bukkit.util.Vector(part.x(),part.y(),part.z()),FurnitureRotation.snap(yaw)));}
    private void tag(Entity entity,SpawnSession session){var p=entity.getPersistentDataContainer();p.set(markerKey,PersistentDataType.BYTE,(byte)1);p.set(groupKey,PersistentDataType.STRING,session.group.toString());p.set(furnitureKey,PersistentDataType.STRING,session.model.id());p.set(ownerKey,PersistentDataType.STRING,session.owner.toString());p.set(protectedKey,PersistentDataType.BYTE,(byte)(session.protectedArea?1:0));}

    public static final class SpawnSession {
        private final SmpFurnitureModel model;private final Location origin;private final float yaw;private final UUID owner;private final boolean protectedArea;private final UUID group;
        private final List<Entity> entities=new ArrayList<>();private int index;private boolean hitboxSpawned;private boolean failed;
        private SpawnSession(SmpFurnitureModel model,Location origin,float yaw,UUID owner,boolean protectedArea,UUID group){this.model=model;this.origin=origin;this.yaw=yaw;this.owner=owner;this.protectedArea=protectedArea;this.group=group;}
        public UUID group(){return group;}public Location origin(){return origin.clone();}
    }
    public record PlacedGroup(UUID id,List<Entity> entities){}
}
