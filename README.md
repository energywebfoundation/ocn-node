# Open Charging Network Client

The Open Charging Network (OCN) Client with OCPI 2.2 connection.

## API Documentation

See [Open Charging Network Client Documentation](https://shareandcharge.bitbucket.io).

## Running a Broker Locally

Import the project in Intellij IDEA after cloning the repository:

```
git clone git@bitbucket.org:shareandcharge/ocn-client.git
cd connect
idea .
```

Next, build and run the client using the `bootRun` task (using the IntelliJ gradle wrapper):

```
./gradlew bootRun
```

Alternatively, you can build and run the compiled `jar` file manually:
```
./gradlew build
java -jar ./build/libs/ocn-client-0.0.1-SNAPSHOT.jar
```

Once it is built and running, test that it is working using the following request:

```
curl localhost:8080/heartbeat
```

You should see a 200 OK response.

### Generating new API documentation

Documentation is generated automatically on build. The asciidoc template can be found in 
`src/docs/asciidoc/index.adoc` and the output in `build/asciidoc/html5/index.html`.