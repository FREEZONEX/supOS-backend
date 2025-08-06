#!/bin/sh

MEM_OPTS="-Xms2g -Xmx3g -Xmn1g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs -XX:NativeMemoryTracking=detail -XX:SurvivorRatio=6 -Dio.netty.noPreferDirect=true -XX:MaxMetaspaceSize=256m -XX:-UseAdaptiveSizePolicy -XX:MaxDirectMemorySize=256m"

JAVA_OPTS="-Dfile.encoding=utf-8 -Dsink.thread=8 -Duser.timezone=UTC -Delk.enabled=false -Djava.security.egd=file:/dev/./urandom -Dlogging.file.name=./logs/supos.log -Dpg.jdbcUrl=jdbc:postgresql://tsdb:5432/postgres -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000"

exec java $MEM_OPTS $JAVA_OPTS -jar /app.jar  # exec 使 Java 成为 PID 1