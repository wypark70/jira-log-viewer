FROM atlassian/jira-software:10.7.4

USER root

# Install curl (Debian-based image assumed) and download PostgreSQL JDBC driver
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl ca-certificates \
  && rm -rf /var/lib/apt/lists/* \
  && mkdir -p /opt/atlassian/jira/lib \
  && curl -fsSL -o /opt/atlassian/jira/lib/postgresql-42.6.0.jar \
       https://repo1.maven.org/maven2/org/postgresql/postgresql/42.6.0/postgresql-42.6.0.jar \
  && chown jira:jira /opt/atlassian/jira/lib/postgresql-42.6.0.jar

USER jira
