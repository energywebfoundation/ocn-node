FROM openjdk:8-jdk-slim

RUN apt-get update && apt-get install -y brotli
COPY build /ocn-node
COPY src/main/resources/* /ocn-node/
WORKDIR /ocn-node


CMD ["java", "-jar", "./libs/ocn-node-1.2.0-rc2.jar"]
