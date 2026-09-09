# MoCap Paper Porting Baseline

## Target

- Platform: Paper
- Minecraft: 1.21.11
- Interface: command-only
- GUI: none
- Scope: behavioral and command parity with the upstream MoCap mod

## Upstream reference

The primary behavioral reference for this port is the upstream MoCap release:

`v1.4-alpha-10+mc1.21.11`

Upstream repository:

`https://github.com/mt1006/mc-mocap-mod`

The implementation is a Paper-native port. Upstream source is used as a behavioral reference; Paper APIs and Paper/Minecraft internals are used where the platform differs.

## Command root

The upstream command root contains these domains:

- `/mocap recording`
- `/mocap playback`
- `/mocap recordings`
- `/mocap scenes`
- `/mocap settings`
- `/mocap misc`
- `/mocap info`
- `/mocap help`

## Implementation order

1. Build system and plugin bootstrap
2. Exact command tree and argument suggestions
3. Recording data model and 20 TPS recorder
4. Recording persistence and file management
5. Playback timeline and playback state machine
6. Player/entity representation and client-visible playback
7. Scene model and scene composition
8. Playback modifiers
9. Settings and persistence
10. Entity, block, inventory, effect, mount, damage and chat tracking/playback
11. Compatibility and edge-case parity
12. Automated tests and Paper 1.21.11 integration validation

No new user-facing features should be introduced during the parity implementation unless they are required internally to reproduce upstream behavior.
