FROM gradle:jdk AS builder

COPY . /ocn-client
WORKDIR /ocn-client

RUN gradle build -Pprofile=docker


FROM openjdk:8

COPY --from=builder /ocn-client/build /ocn-client
COPY --from=builder /ocn-client/src/main/resources/* /ocn-client/
WORKDIR /ocn-client

CMD ["java", "-jar", "./libs/ocn-client-0.1.0-SNAPSHOT.jar"]
