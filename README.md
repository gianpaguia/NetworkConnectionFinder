# NetworkConnectionFinder
Network Connection Finder - DSA

Social network connection finder: JavaFX desktop app backed by SQLite, with a
student friendship graph (adjacency list), BFS pathfinding, degree-centrality
metrics, and profile-based study group matching.

## Build & Run

The Maven wrapper (`mvnw.cmd`) self-bootstraps Apache Maven 3.9.9 into `.tools\`
on first use (single download, no prior Maven installation required):

```
mvnw.cmd test            # run the unit + integration tests
mvnw.cmd javafx:run      # launch the JavaFX application
```

Defaults seeded on first run: admin login `admin` / `admin123`, students (IDs
`2026-10001`..`2026-10010`) login with `password123`.

The SQLite database lives at `data\network_finder.db` and is created on demand.
Tests run against an isolated temporary database and never touch the real one.
