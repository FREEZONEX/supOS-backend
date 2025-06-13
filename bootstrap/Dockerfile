FROM maven:3.8.5-openjdk-8 AS builder
WORKDIR /app
COPY / /app
RUN mvn clean package -DskipTests
FROM eco-registry.supos.com/shengtai/openjdk8:jdk8u312
#ADD bootstrap-1.0-SNAPSHOT.jar app.jar
COPY --from=builder /app/bootstrap/target/bootstrap-1.0-SNAPSHOT.jar app.jar
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone

RUN mkdir -p /data/apps /data/resource/i18n

# copy i18n properties to /data/resource/i18n
RUN cd /tmp/ && jar -xf /app.jar && cp /tmp/BOOT-INF/classes/i18n/*.properties /data/resource/i18n

EXPOSE 8080 19099
ENTRYPOINT ["sh","-c","java $MEM_OPTS $JAVA_OPTS  -Djava.security.egd=file:/dev/./urandom -Dlogging.file.name=./logs/supos.log -jar /app.jar"]