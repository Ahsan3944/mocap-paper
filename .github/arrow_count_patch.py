from pathlib import Path
import re


def replace(path, old, new):
    p = Path(path)
    s = p.read_text()
    if old not in s:
        raise SystemExit(f'pattern not found in {path}: {old[:80]}')
    p.write_text(s.replace(old, new, 1))

# Player frames: parity with upstream SET_ARROW_COUNT (arrows stuck in body + bee stingers).
p = 'src/main/java/com/ultraop/mocap/recording/PlayerStateFrame.java'
s = Path(p).read_text()
s = s.replace('boolean spectator,boolean closeContainer) implements', 'boolean spectator,boolean closeContainer,int arrowCount,int stingerCount) implements', 1)
s = s.replace('false,false);}\n    public PlayerStateFrame(long tick', 'false,false,0,0);}\n    public PlayerStateFrame(long tick', 1)
s = s.replace('activeItemRemainingTime,false,false);}\n    public static PlayerStateFrame capture', 'activeItemRemainingTime,false,false,0,0);}\n    public static PlayerStateFrame capture', 1)
s = s.replace('player.getGameMode()==org.bukkit.GameMode.SPECTATOR,closeContainer);', 'player.getGameMode()==org.bukkit.GameMode.SPECTATOR,closeContainer,player.getArrowsStuck(),player.getBeeStingerCount());', 1)
Path(p).write_text(s)

# Entity frames: same state is an upstream comparable action for every LivingEntity.
p = 'src/main/java/com/ultraop/mocap/recording/EntityStateFrame.java'
s = Path(p).read_text()
s = s.replace('UUID vehicleId, boolean hurt, String nbt)', 'UUID vehicleId, boolean hurt, String nbt, int arrowCount, int stingerCount)', 1)
s = s.replace('vehicleId,false,null);}', 'vehicleId,false,null,0,0);}', 1)
s = s.replace('vehicleId,hurt,null);}', 'vehicleId,hurt,null,0,0);}', 1)
s = s.replace('vehicleId,hurt,nbt);', 'vehicleId,hurt,nbt,living==null?0:living.getArrowsStuck(),living==null?0:living.getBeeStingerCount());', 1)
Path(p).write_text(s)

# Playback applies both values every state frame.
p = 'src/main/java/com/ultraop/mocap/playback/EntityPlaybackActor.java'
s = Path(p).read_text()
needle = 'living.setPose(frame.pose());living.setFallDistance(frame.fallDistance());living.setInvulnerable'
replacement = 'living.setPose(frame.pose());living.setFallDistance(frame.fallDistance());living.setArrowsStuck(frame.arrowCount());living.setBeeStingerCount(frame.stingerCount());living.setInvulnerable'
count = s.count(needle)
if count != 2:
    raise SystemExit(f'expected 2 playback frame sites, found {count}')
s = s.replace(needle, replacement)
Path(p).write_text(s)

# Recording format v15 stores the two new state values while remaining backward-compatible.
p = 'src/main/java/com/ultraop/mocap/recording/RecordingRepository.java'
s = Path(p).read_text()
s = s.replace('private static final int FORMAT_VERSION=14', 'private static final int FORMAT_VERSION=15', 1)
s = s.replace('readFrame(o,version>=7,version>=11,version>=12,version>=13,ps)', 'readFrame(o,version>=7,version>=11,version>=12,version>=13,version>=15,ps)', 1)
s = s.replace('readEntityFrame(o,version>=7,version>=10,version>=11,es)', 'readEntityFrame(o,version>=7,version>=10,version>=11,version>=15,es)', 1)
s = s.replace('o.writeBoolean(f.spectator());o.writeBoolean(f.closeContainer());}', 'o.writeBoolean(f.spectator());o.writeBoolean(f.closeContainer());o.writeInt(f.arrowCount());o.writeInt(f.stingerCount());}', 1)
s = s.replace('boolean hasClose,double[] start)throws IOException', 'boolean hasClose,boolean hasEffects,double[] start)throws IOException', 1)
s = s.replace('boolean close=hasClose&&in.readBoolean();return new PlayerStateFrame(', 'boolean close=hasClose&&in.readBoolean();int arrows=hasEffects?in.readInt():0;int stingers=hasEffects?in.readInt():0;return new PlayerStateFrame(', 1)
s = s.replace('active,hand,activeItem,used,remaining,spectator,close);', 'active,hand,activeItem,used,remaining,spectator,close,arrows,stingers);', 1)
s = s.replace('o.writeBoolean(f.hurt());o.writeBoolean(f.nbt()!=null);if(f.nbt()!=null)o.writeUTF(f.nbt());}', 'o.writeBoolean(f.hurt());o.writeBoolean(f.nbt()!=null);if(f.nbt()!=null)o.writeUTF(f.nbt());o.writeInt(f.arrowCount());o.writeInt(f.stingerCount());}', 1)
s = s.replace('boolean hasNbt,boolean compact,double[] start)throws IOException', 'boolean hasNbt,boolean compact,boolean hasEffects,double[] start)throws IOException', 1)
s = s.replace('String nbt=hasNbt&&in.readBoolean()?in.readUTF():null;return new EntityStateFrame(', 'String nbt=hasNbt&&in.readBoolean()?in.readUTF():null;int arrows=hasEffects?in.readInt():0;int stingers=hasEffects?in.readInt():0;return new EntityStateFrame(', 1)
s = s.replace('vehicle,hurt,nbt);', 'vehicle,hurt,nbt,arrows,stingers);', 1)
Path(p).write_text(s)
