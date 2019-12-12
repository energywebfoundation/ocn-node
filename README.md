# Open Charging Network Node

The Open Charging Network (OCN) node with Open Charge Point Interface (OCPI) v2.2 [RC2] API. 

**This project replaces the OCN Client, with the rename seeking to avoid confusion with client-server architecture.**

Changelist from rename:

- New registry contract address (`0x0A5f27Ee1EbDC68034aDbd9446F9375783aeF7DC`)
- registry methods renamed:
    - `updateClientInfo` -> `updateNodeInfo`
    - `clientAddressOf` -> `nodeAddressOf`
    - `clientURLOf` -> `nodeURLOf`
- `/ocn/registry/client-info` endpoint renamed to `/ocn/registry/node-info`


**This software is in alpha**. 

As the aim is for this to be a community project, contributions are always welcome in the form of comments, pull 
requests and raised issues. Questions may also be asked on Stack Overflow using the tag `ShareAndCharge`, or in the
Gitter [community](https://gitter.im/shareandcharge/community).

## Open Charging Network

To participate in the OCN, a node must be used to broker OCPI requests (e.g. start/stop requests, POI data retrieval) 
between parties. There are two ways to use a node. Either it is run on-premises by an administrator working for the
OCPI party wishing to participate in the network, or it is provided as a Service by an OCN Node Provider. If the 
latter scenario is desired, the OCPI party needs only to obtain a `CREDENTIALS_TOKEN_A` and versions endpoint from said 
provider to begin the regular OCPI 2.2 credentials registration process with the provider's OCN Node.  

For more information about the OCN, check out the [wiki](https://bitbucket.org/shareandcharge/ocn-node/wiki/).

## API Documentation

See [Open Charging Network Node Documentation](https://shareandcharge.bitbucket.io).

## Dependencies

The OCN Node is built with Kotlin, targeting the JVM. Either OpenJDK 8 or Docker can be used to build and run the node.
If following the Local OCN tutorial below, it is only necessary to have Docker installed.

## Running a Local Open Charging Network

Before running a node and connecting it to a local, test or prod environment, it is recommended to become acquainted 
with how the network operates first. The `docker-compose` file provided spins up a local environment with the OCN
Registry and two OCN Nodes already pre-configured. A [tutorial](./examples) has been provided to guide 
administrators and users of an OCN Node alike through various use case examples.  

## Running a Node

First of all, clone the repository:

```
git clone git@bitbucket.org:shareandcharge/ocn-node.git
cd ocn-node
```

### 1. Modifying Node Configuration

Firstly, it is important to configure the application properties for the desired environment or profile, e.g. `local`:

```
cd src/main/resources
cp application.dev.properties application.local.properties
vi application.local.properties
```

#### 1.1. Setting the Node Address

The field `ocn.node.url` describes the node's server address. If running a local network for development purposes,
setting the following:

```
ocn.node.url = http://localhost:8080
``` 

Results in requesting platforms obtaining OCPI module endpoints starting with `http://localhost:8080`, for 
example `http://localhost:8080/ocpi/2.2/sender/locations` for the locations module's sender interface.
 
Likewise, for a public node on the test network, the url would be `https://server.example.com`. This would translate
to `https://server.example.com/ocpi/2.2/sender/locations`. Be sure to include the protocol so that connected platforms
can correctly parse the endpoints provided.

#### 1.2. Connecting to a Database

The `dev` properties connects the node to an in-memory database, which will not persist data across node restarts.
If running the node in a test or production environment with Postgres installed, copy from the `psql` properties file 
instead:

```
cp application.psql.properties application.local.properties
```

The node may also be connected to a different database. This requires installing the relevant database driver as 
an `ocn-node` dependency via gradle and modifying application properties accordingly, in addition to running the 
database server itself. 

#### 1.3. Configuring the Network

The network on which any OCN Node is running on depends purely on the OCN Registry smart contract it is connected to.
These configuration properties belong to `ocn.node.web3`. Currently the `develop` branch is pre-configured (using the
provided `dev` and `psql` profiles) to use to the Energy Web Foundation's Volta test network, by connecting to a [remote Volta node](https://energyweb.atlassian.net/wiki/spaces/EWF/pages/703201459/Volta+Connecting+to+Remote+RPC) 
and the OCN Registry smart contract deployed on Volta with address `0x0A5f27Ee1EbDC68034aDbd9446F9375783aeF7DC`. Note
that subsequent commits may change this address as development of the OCN Registry takes place.

#### 1.4. Setting the Admin API key [optional]

The Admin API allows, for example, generating new OCPI tokens (`CREDENTIALS_TOKEN_A`) for planned platforms. An API
key can be set in the application's properties, else a new one will be generated on restart:

```
ocn.node.apikey = randomkey
```

The API key will be printed on node start, be it generated or user-specified. Consult the [API documentation](https://shareandcharge.bitbucket.io)
for more information on how to use the Admin API. 

### 2. Running the Node

There are multiple ways to run an OCN Node. First of all, return to the root of the repository:

```
cd ../../..
```

Or you can enter the following shortcut to return to the previous directory, which should be the root if following
these instructions:

```
cd -
```

#### 2.1. Building and Executing a JAR file

To build the node with the desired profile (relating to the `application.<PROFILE>.properties` file), for example a
`local` profile, run the following:

```
./gradlew -Pprofile=local build
```

Once built, the packaged node can be run using:
```
java -jar ./build/libs/ocn-node-0.1.0-SNAPSHOT.jar
```

Alternatively, a different profile can be selected at runtime, e.g.:
```
java -jar -Dspring.config.location=/path/to/application.prod.properties ./build/libs/ocn-node-0.1.0-SNAPSHOT.jar
```

#### 2.2. Using the Gradle Wrapper

Especially helpful for development, the node can quickly be run in one step with Gradle using the provided wrapper:

```
./gradlew bootRun -Pprofile=<PROFILE>
```

#### 2.3. Using Docker

A Dockerfile is provided which, once built, will run the above command in a container. To build, simply run the following
with a name flag so that we can identify it later:

```
docker build . -n ocn-node
```

Note that building the docker image can take a few minutes.

Once built, the node can be run, exposing the application server's `8080` port within the container to the outside environment:
```
docker run -p 8080:8080 ocn-node 
```

By default this will use the `docker` profile. This can be changed by modifying the Dockerfile directly (and rebuilding
if necessary), or by changing the location of the properties file at runtime with a custom command, as shown below:

```
docker run -p 8080:8080 ocn-node java -jar -Dspring.config.location=resources/main/application.<PROFILE>.properties lib/ocn-node-0.1.0-SNAPSHOT.jar
```

### 3. Operating the OCN Node

Once the node is running, test that it is working with the following request:

```
curl localhost:8080/health
```

You should see a 200 OK response.

For further usage documentation, consult the [API Documentation](https://shareandcharge.bitbucket.io).

### Generating new API documentation

Documentation is generated automatically on build. The asciidoc template can be found in 
`src/docs/asciidoc/index.adoc` and the output in `build/asciidoc/html5/index.html`.