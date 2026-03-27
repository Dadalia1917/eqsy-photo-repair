FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY ruoyi-admin/pom.xml ruoyi-admin/pom.xml
COPY ruoyi-common/pom.xml ruoyi-common/pom.xml
COPY ruoyi-framework/pom.xml ruoyi-framework/pom.xml
COPY ruoyi-system/pom.xml ruoyi-system/pom.xml
COPY ruoyi-generator/pom.xml ruoyi-generator/pom.xml
COPY ruoyi-quartz/pom.xml ruoyi-quartz/pom.xml

COPY ruoyi-admin/src ruoyi-admin/src
COPY ruoyi-common/src ruoyi-common/src
COPY ruoyi-framework/src ruoyi-framework/src
COPY ruoyi-system/src ruoyi-system/src
COPY ruoyi-generator/src ruoyi-generator/src
COPY ruoyi-quartz/src ruoyi-quartz/src

RUN mvn -T 1C -DskipTests clean package -pl ruoyi-admin -am

FROM eclipse-temurin:17-jre
LABEL version="1.0.3" maintainer="eqsy"
WORKDIR /app

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

COPY --from=builder /workspace/ruoyi-admin/target/ruoyi-admin.jar /app/ruoyi-admin.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/ruoyi-admin.jar"]
