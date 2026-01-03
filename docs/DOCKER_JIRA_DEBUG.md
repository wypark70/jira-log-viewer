# Docker Jira Debug (Linux/Windows)

This guide shows how to run a Jira container via Docker Compose, mount the built plugin, and attach an IDE debugger on port 5005.

WARNING: Official Atlassian Docker images may have licensing or distribution restrictions. Replace the image in `docker/docker-compose.yml` with an image you are allowed to use.

1) Build plugin and copy to docker plugin folder
```powershell
# From project root
.\gradlew.bat :plugin:copyPluginToDocker
```
This copies the generated `*.jar` from `plugin/build/libs/` into `docker/jira/plugins`.

2) Start Jira container
```powershell
# From project root
docker compose -f docker/docker-compose.yml up -d
```
Or use the Gradle helper task (requires Docker installed):
```powershell
.\gradlew.bat :plugin:dockerComposeUp
```

3) Verify Jira starts
- Visit http://localhost:8080
- Logs: `docker compose logs -f jira`

4) Attach debugger
- Host: `localhost`
- Port: `5005`
- Configure a Remote JVM Debug configuration in your IDE.

5) Update plugin
- Rebuild and copy:
```powershell
.\gradlew.bat :plugin:copyPluginToDocker
```
- Then either reload the plugin from Jira admin UI or restart the container:
```powershell
docker compose -f docker/docker-compose.yml restart jira
```

6) Stop the container
```powershell
docker compose -f docker/docker-compose.yml down
```
Or use Gradle helper:
```powershell
.\gradlew.bat :plugin:dockerComposeDown
```

Notes
- If Jira image expects plugins in a different folder, adjust `docker-compose.yml` volume mapping accordingly.
- If the container's JVM does not pick up the `JVM_SUPPORT_RECOMMENDED_ARGS` env trick for debugging, you may need to customize the container start command or image to include debug options in `JAVA_OPTS`.
- If your environment uses `docker` vs `docker compose` commands, adapt the commands accordingly.
