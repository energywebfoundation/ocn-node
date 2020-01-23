FROM openjdk:8-alpine

COPY build /ocn-node
COPY src/main/resources/* /ocn-node/
WORKDIR /ocn-node

CMD ["java", "-jar", "./libs/ocn-node-0.1.0-SNAPSHOT.jar"]
