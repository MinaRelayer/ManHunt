# ManHunt

A Hunter-vs-Runner mini-game plugin for Minecraft Paper 26.1.2.

## Features

- Hunter, Runner, and Spectator teams.
- Explicit `WAITING`, `STARTING`, `RUNNING`, `ENDED`, and `CLEANUP` lifecycle states.
- Temporary Paper namespaced worlds under the primary world's `dimensions` directory.
- Safe ownership markers, path validation, and asynchronous cleanup.
- Full player-state isolation and restoration.
- Runner disconnect grace period.
- Ender Dragon victory detection limited to the match End.
- Player-local scoreboard showing role, time, and alive Runners without visible row numbers.
- `/mh tp` is restricted to Spectators during an active match.
- English and Simplified Chinese localization.
- Optional PlaceholderAPI expansion.

## Requirements

- Paper 26.1.2
- Java 25
- PlaceholderAPI 2.11.6 (optional)

## Installation

1. Build or download `ManHunt-1.0.1.jar`.
2. Copy it into the server `plugins` directory.
3. Start the server once and edit `plugins/ManHunt/config.yml` if needed.
4. Restart the server after changing world or match settings.


## Configuration

```yaml
language: zh_cn

world:
  namespace: manhunt
  seed: random
  difficulty: HARD

game:
  disconnect-grace-seconds: 300

scoreboard:
  refresh-ticks: 20
```

Temporary worlds are stored below the primary world's Paper dimensions directory:

```text
world/dimensions/manhunt/game_<id>_world
world/dimensions/manhunt/game_<id>_the_nether
world/dimensions/manhunt/game_<id>_the_end
```

## Commands

| Command | Description |
| --- | --- |
| `/mh` | Show help. |
| `/mh runner` | Join Runner while waiting. |
| `/mh hunter` | Join Hunter while waiting. |
| `/mh spectator` | Join Spectator while waiting. |
| `/mh runner <player>` | Assign Runner. Admin only. |
| `/mh hunter <player>` | Assign Hunter. Admin only. |
| `/mh spectator <player>` | Assign Spectator. Admin only. |
| `/mh start` | Start a match. Admin only. |
| `/mh stop` | Stop a match. Admin only. |
| `/mh reload` | Reload configuration while waiting. Admin only. |
| `/mh tp <player>` | Teleport to a participant. Spectators only during a match. |

## Permissions

```text
manhunt.admin
```

This permission is granted to operators by default. It does not grant `/mh tp`; teleporting is Spectator-only.

## PlaceholderAPI

Identifier: `manhunt`

```text
%manhunt_team%
%manhunt_state%
%manhunt_runners_alive%
%manhunt_hunter_count%
%manhunt_runner_count%
%manhunt_spectator_count%
%manhunt_game_time%
%manhunt_winner%
%manhunt_player_alive%
```

## Building

Windows:

```powershell
.\gradlew.bat clean test jar
```

Linux/macOS:

```bash
./gradlew clean test jar
```

Output:

```text
build/libs/ManHunt-1.0.1.jar
```

## Releases

Every version is published as a GitHub Release. To publish a new version:

1. Update `version` in `gradle.properties` and the matching changelog entry.
2. Run the complete verification suite.
3. Create and push a version tag, for example:

```bash
git tag v1.0.1
git push origin v1.0.1
```

The combined GitHub Actions workflow validates the tag, runs the tests, builds the plugin, and attaches `ManHunt-<version>.jar` to the GitHub Release with generated release notes.

## License

MIT License. See [LICENSE](LICENSE).
