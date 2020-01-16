FROM gradle:jdk8 AS builder

COPY . /ocn-node
WORKDIR /ocn-node

RUN gradle build -Pprofile=docker


FROM openjdk:8

COPY --from=builder /ocn-node/build /ocn-node
COPY --from=builder /ocn-node/src/main/resources/* /ocn-node/
WORKDIR /ocn-node

CMD ["java", "-jar", "./libs/ocn-node-0.1.0-SNAPSHOT.jar"]
