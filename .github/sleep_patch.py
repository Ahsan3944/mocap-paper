from pathlib import Path

# SLEEP parity: persist LivingEntity sleeping position.
p='src/main/java/com/ultraop/mocap/recording/PlayerStateFrame.java'
s=Path(p).read_text()
s=s.replace('boolean spectator,boolean closeContainer,int arrowCount,int stingerCount)', 'boolean spectator,boolean closeContainer,int arrowCount,int stingerCount,boolean sleeping,int sleepX,int sleepY,int sleepZ)', 1)
s=s.replace('false,false,0,0);}', 'false,false,0,0,false,0,0,0);}', 1)
s=s.replace('activeItemRemainingTime,false,false,0,0);', 'activeItemRemainingTime,false,false,0,0,false,0,0,0);', 1)
old='boolean active=player.hasActiveItem();EquipmentSlot hand=active?player.getActiveItemHand():null;ItemStack activeItem=active?cloneItem(player.getActiveItem()):null;int used=active?player.getActiveItemUsedTime():0,remaining=active?player.getActiveItemRemainingTime():0;return new PlayerStateFrame('
new='boolean active=player.hasActiveItem();EquipmentSlot hand=active?player.getActiveItemHand():null;ItemStack activeItem=active?cloneItem(player.getActiveItem()):null;int used=active?player.getActiveItemUsedTime():0,remaining=active?player.getActiveItemRemainingTime():0;net.minecraft.core.BlockPos bed=((org.bukkit.craftbukkit.entity.CraftPlayer)player).getHandle().getSleepingPos().orElse(null);return new PlayerStateFrame('
if old not in s: raise SystemExit('player capture needle missing')
s=s.replace(old,new,1)
s=s.replace('closeContainer,player.getArrowsInBody(),player.getBeeStingersInBody());}', 'closeContainer,player.getArrowsInBody(),player.getBeeStingersInBody(),bed!=null,bed==null?0:bed.getX(),bed==null?0:bed.getY(),bed==null?0:bed.getZ());}', 1)
Path(p).write_text(s)

p='src/main/java/com/ultraop/mocap/recording/EntityStateFrame.java'
s=Path(p).read_text()
s=s.replace('String nbt, int arrowCount, int stingerCount)', 'String nbt, int arrowCount, int stingerCount, boolean sleeping, int sleepX, int sleepY, int sleepZ)', 1)
s=s.replace('vehicleId,false,null,0,0);}', 'vehicleId,false,null,0,0,false,0,0,0);}', 1)
s=s.replace('vehicleId,hurt,null,0,0);}', 'vehicleId,hurt,null,0,0,false,0,0,0);}', 1)
old='Location location=entity.getLocation(); LivingEntity living=entity instanceof LivingEntity value?value:null; EntityEquipment equipment='
new='Location location=entity.getLocation(); LivingEntity living=entity instanceof LivingEntity value?value:null; net.minecraft.core.BlockPos bed=living==null?null:((net.minecraft.world.entity.LivingEntity)((org.bukkit.craftbukkit.entity.CraftEntity)entity).getHandle()).getSleepingPos().orElse(null); EntityEquipment equipment='
if old not in s: raise SystemExit('entity capture needle missing')
s=s.replace(old,new,1)
s=s.replace('living==null?0:living.getBeeStingersInBody());}', 'living==null?0:living.getBeeStingersInBody(),bed!=null,bed==null?0:bed.getX(),bed==null?0:bed.getY(),bed==null?0:bed.getZ());}', 1)
Path(p).write_text(s)

p='src/main/java/com/ultraop/mocap/playback/EntityPlaybackActor.java'
s=Path(p).read_text()
s=s.replace('living.setPose(frame.pose());living.setFallDistance(frame.fallDistance());living.setArrowsInBody(frame.arrowCount());living.setBeeStingersInBody(frame.stingerCount());', 'living.setPose(frame.pose());living.setFallDistance(frame.fallDistance());living.setArrowsInBody(frame.arrowCount());living.setBeeStingersInBody(frame.stingerCount());applySleeping(living,frame.sleeping(),frame.sleepX(),frame.sleepY(),frame.sleepZ());', 2)
needle='    private void sendFluentMovement()'
helper='    private static void applySleeping(LivingEntity living,boolean sleeping,int x,int y,int z){try{var nms=(net.minecraft.world.entity.LivingEntity)((CraftEntity)living).getHandle();if(sleeping)nms.setSleepingPos(new net.minecraft.core.BlockPos(x,y,z));else nms.clearSleepingPos();}catch(Exception ignored){}}\n'
if needle not in s: raise SystemExit('playback needle missing')
s=s.replace(needle,helper+needle,1)
Path(p).write_text(s)

p='src/main/java/com/ultraop/mocap/recording/RecordingRepository.java'
s=Path(p).read_text()
s=s.replace('private static final int FORMAT_VERSION=15', 'private static final int FORMAT_VERSION=16', 1)
s=s.replace('version>=15,ps)', 'version>=15,version>=16,ps)', 1)
s=s.replace('version>=15,es)', 'version>=15,version>=16,es)', 1)
s=s.replace('o.writeInt(f.arrowCount());o.writeInt(f.stingerCount());}', 'o.writeInt(f.arrowCount());o.writeInt(f.stingerCount());o.writeBoolean(f.sleeping());o.writeInt(f.sleepX());o.writeInt(f.sleepY());o.writeInt(f.sleepZ());}', 1)
s=s.replace('boolean hasClose,boolean hasEffects,double[] start', 'boolean hasClose,boolean hasEffects,boolean hasSleep,double[] start', 1)
s=s.replace('int stingers=hasEffects?in.readInt():0;return new PlayerStateFrame(', 'int stingers=hasEffects?in.readInt():0;boolean sleeping=hasSleep&&in.readBoolean();int sleepX=hasSleep?in.readInt():0;int sleepY=hasSleep?in.readInt():0;int sleepZ=hasSleep?in.readInt():0;return new PlayerStateFrame(', 1)
s=s.replace('close,arrows,stingers);', 'close,arrows,stingers,sleeping,sleepX,sleepY,sleepZ);', 1)
s=s.replace('o.writeInt(f.arrowCount());o.writeInt(f.stingerCount());}private static EntityStateFrame', 'o.writeInt(f.arrowCount());o.writeInt(f.stingerCount());o.writeBoolean(f.sleeping());o.writeInt(f.sleepX());o.writeInt(f.sleepY());o.writeInt(f.sleepZ());}private static EntityStateFrame', 1)
s=s.replace('boolean hasNbt,boolean compact,boolean hasEffects,double[] start', 'boolean hasNbt,boolean compact,boolean hasEffects,boolean hasSleep,double[] start', 1)
s=s.replace('int stingers=hasEffects?in.readInt():0;return new EntityStateFrame(', 'int stingers=hasEffects?in.readInt():0;boolean sleeping=hasSleep&&in.readBoolean();int sleepX=hasSleep?in.readInt():0;int sleepY=hasSleep?in.readInt():0;int sleepZ=hasSleep?in.readInt():0;return new EntityStateFrame(', 1)
s=s.replace('nbt,arrows,stingers);', 'nbt,arrows,stingers,sleeping,sleepX,sleepY,sleepZ);', 1)
Path(p).write_text(s)
