# Changelog

## 1.0.3 - 2026-08-02

- Use timestamp-based temporary world names.
- Keep the hunter compass movable in the inventory and offhand while blocking drops and containers.


## 1.0.2 - 2026-08-02

- Added an enchanted, protected hunter compass with per-world runner tracking.
- Allowed the hunter compass to move freely, including the offhand, while preventing drops.
- Separated player and administrator help output.
- Kept console log messages in English.
- Preserved the scoreboard after victory and froze its final game time until `/mh stop`.


## 1.0.1 - 2026-08-02

- Refactored match lifecycle and player-state isolation.
- Added safe temporary Paper world cleanup.
- Fixed scoreboard team packet desynchronization.
- Fixed Spectator respawn handling.
- Restricted `/mh tp` to Spectators.
- Added GitHub Actions, Gradle Wrapper, documentation, and MIT licensing.
