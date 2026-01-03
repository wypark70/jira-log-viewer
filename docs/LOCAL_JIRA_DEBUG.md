# Local Jira Debug (Windows)

This document explains how to run a local Jira instance on Windows and attach an IDE debugger (port 5005).

Prerequisites
- A Jira distribution (ZIP) compatible with your plugin (download from Atlassian). Extract to a folder, e.g. `C:\atlassian\jira`.
- Java (matching Jira requirements) installed and on PATH.
- Build the plugin JAR using Gradle.

1) Build plugin JAR
```powershell
# From project root
.\gradlew.bat :plugin:jar
# Output: plugin\build\libs\<your-plugin>.jar
```

2) Deploy plugin JAR to local Jira
- Option A: use provided Gradle task (recommended)
```powershell
# Provide the plugin installation directory where Jira reads installed plugins
.\gradlew.bat :plugin:deployToLocalJira -PlocalJiraPluginsDir="C:/path/to/jira-home/plugins/installed-plugins"
# or set env var
$env:LOCAL_JIRA_PLUGINS_DIR = 'C:/path/to/jira-home/plugins/installed-plugins'
.\gradlew.bat :plugin:deployToLocalJira
```

- Option B: copy manually
```powershell
copy .\plugin\build\libs\*.jar "C:\path\to\jira-home\plugins\installed-plugins\"
```

3) Enable remote debugging for Jira
- Edit `C:\path\to\jira\bin\setenv.bat` and add the JVM debug agent to `JAVA_OPTS`:
```
set JAVA_OPTS=%JAVA_OPTS% -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
```
- If you prefer the JVM to wait for the debugger, use `suspend=y`.

4) Start Jira
```powershell
cd C:\path\to\jira\bin
start-jira.bat
```

5) Attach debugger in your IDE
- Host: `localhost`
- Port: `5005`
- Use an IntelliJ Remote JVM Debug configuration.

Notes
- When you update the JAR, copy it again to the `installed-plugins` folder and use Jira admin to reload the plugin or restart Jira if needed.
- If your Jira distribution keeps plugins in a different location, use that directory instead.
- For Windows, ensure you run start/stop scripts with appropriate permissions.

Troubleshooting
- If Jira fails to pick up the plugin, check `atlassian-jira.log` and `catalina.out` (or corresponding logs) in the Jira home directory.
- If `deployToLocalJira` fails due to permissions, run PowerShell as Administrator or choose a writable folder.
