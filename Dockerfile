FROM maven:amazoncorretto AS builder
WORKDIR /app
COPY / /app
RUN mvn clean package -DskipTests

RUN ls -l /app/bootstrap/target/

FROM ibm-semeru-runtimes:open-17-jdk

COPY  --from=builder /app/bootstrap/target/bootstrap*.jar /app.jar
RUN pwd
RUN ls -l /app.jar

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone

RUN mkdir -p /data/apps /data/resource/i18n

# copy i18n properties to /data/resource/i18n
RUN cd /tmp/ && jar -xf /app.jar && cp /tmp/BOOT-INF/classes/i18n/*.properties /data/resource/i18n

EXPOSE 8080 8080
ENTRYPOINT ["sh","-c","java $MEM_OPTS $JAVA_OPTS  -Djava.security.egd=file:/dev/./urandom -Dlogging.file.name=./logs/supos.log -jar /app.jar"]