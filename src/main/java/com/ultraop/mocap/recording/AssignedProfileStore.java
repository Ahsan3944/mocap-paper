package com.ultraop.mocap.recording;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

/** Stores the optional profile assigned to a recording. */
final class AssignedProfileStore {
    private final Path directory;
    AssignedProfileStore(Path directory) { this.directory = directory; }
    void save(String name, RecordingSession session) throws IOException {
        RecordingSession.AssignedProfile p = session.getAssignedProfile();
        Path file = pathFor(name);
        if (p == null || p.name() == null) { Files.deleteIfExists(file); return; }
        Files.createDirectories(directory);
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeUTF(p.name()); out.writeBoolean(p.id() != null); if (p.id() != null) out.writeUTF(p.id().toString());
            writeNullable(out, p.skinValue()); writeNullable(out, p.skinSignature());
        }
    }
    RecordingSession.AssignedProfile load(String name) throws IOException {
        Path file = pathFor(name); if (!Files.isRegularFile(file)) return null;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            String profileName = in.readUTF(); UUID id = in.readBoolean() ? UUID.fromString(in.readUTF()) : null;
            return new RecordingSession.AssignedProfile(profileName, id, readNullable(in), readNullable(in));
        } catch (IllegalArgumentException | EOFException e) { throw new IOException("Invalid assigned profile: " + name, e); }
    }
    void delete(String name) throws IOException { Files.deleteIfExists(pathFor(name)); }
    void copy(String oldName, String newName) throws IOException { Path s=pathFor(oldName),t=pathFor(newName); if(Files.isRegularFile(s)){Files.createDirectories(directory);Files.copy(s,t,StandardCopyOption.REPLACE_EXISTING);} }
    void rename(String oldName, String newName) throws IOException { Path s=pathFor(oldName),t=pathFor(newName); if(Files.isRegularFile(s)){Files.createDirectories(directory);Files.move(s,t,StandardCopyOption.REPLACE_EXISTING);} }
    private Path pathFor(String name) { return directory.resolve(name + ".profile").normalize(); }
    private static void writeNullable(DataOutputStream out,String v)throws IOException{out.writeBoolean(v!=null);if(v!=null)out.writeUTF(v);}
    private static String readNullable(DataInputStream in)throws IOException{return in.readBoolean()?in.readUTF():null;}
}
