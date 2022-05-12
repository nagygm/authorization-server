#!/usr/bin/env sh
echo "Starting Application ..."
java -Dserver.port=$PORT -Dspring.profiles.active=cloud,cloud-$PROFILE -jar app.jar

