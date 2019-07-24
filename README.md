# Open Charging Network Client

The Open Charging Network (OCN) Client with OCPI 2.2 connection. 

**This software is in alpha**. Starting from end of July, a testing period will be run to aid and inform development.
Additionally, contributions are always welcome in the form of pull requests and issues.

## API Documentation

See [Open Charging Network Client Documentation](https://shareandcharge.bitbucket.io).

## Running a Client Locally

First of all, clone the repository:

```
git clone git@bitbucket.org:shareandcharge/ocn-client.git
cd ocn-client
```

### 1. Modifying Client Configuration

Firstly, it is important to configure the application properties for the desired environment or profile, e.g. `local`:

```
cd src/main/resources
cp application.dev.properties application.local.properties
vi application.local properties
```

#### 1.1. Setting the Client Address

The field `ocn.client.url` describes the client's server address. If running a local network for development purposes,
setting the following:

```
ocn.client.url = http://localhost:8080
``` 

Results in requesting platforms obtaining OCPI module endpoints starting with `http://localhost:8080`, for 
example `http://localhost:8080/ocpi/2.2/sender/locations` for the locations module's sender interface.
 
Likewise, for a public node on the test network, the url would be `https://server.example.com`. This would translate
to `https://server.example.com/ocpi/2.2/sender/locations`. Be sure to include the protocol so that connected platforms
can correctly parse the endpoints provided.

#### 1.2. Connecting to a Database

The `dev` properties connects the client to an in-memory database, which will not persist data across client restarts.
If running the client in a test or production environment with Postgres installed, copy from the `psql` properties file 
instead:

```
cd application.psql.properties application.local.properties
```

The client may also be connected to a different database. This requires installing the relevant database driver as 
an `ocn-client` dependency via gradle and modifying application properties accordingly, in addition to running the 
database server itself. 

#### 1.3. Configuring the Network

The network on which any OCN client is running on depends purely on the OCN Registry smart contract it is connected to.
These configuration properties belong to `ocn.client.web3`. Currently the OCN test environment exists on the Energy Web 
Foundation's Volta test network. The provided `dev` and `psql` profiles already point the client to a [remote Volta node](https://energyweb.atlassian.net/wiki/spaces/EWF/pages/703201459/Volta+Connecting+to+Remote+RPC) 
and to the OCN Registry smart contract deployed on Volta (with address `0x668956FE2Eb6ED52C5a961b02bEEbAc8913A2731`).

#### 1.4. Setting the Admin API key [optional]

The Admin API allows, for example, generating new OCPI tokens (`CREDENTIALS_TOKEN_A`) for planned platforms. An API
key can be set in the application's properties, else a new one will be generated on restart:

```
ocn.client.apikey = randomkey
```

The API key will be printed on client start, be it generated or user-specified. Consult the [API documentation](https://shareandcharge.bitbucket.io)
for more information on how to use the Admin API. 

### 2. Building and Running the Client

There are multiple ways to run an OCN client.

#### 2.1. Building and Executing a JAR file

To build the client with the desired profile (relating to the `application.<PROFILE>.properties` file), for example a
`local` profile, run the following:

```
./gradlew -Pprofile=local build
```

Once built, the packaged client can be run using:
```
java -jar ./build/libs/ocn-client-0.1.0-SNAPSHOT.jar
```

Alternatively, a different profile can be selected at runtime, e.g.:
```
java -jar -Dspring.config.location=/path/to/application.prod.properties ./build/libs/ocn-client-0.1.0-SNAPSHOT.jar
```

#### 2.2. Using the Gradle Wrapper

Especially helpful for development, the client can quickly be run in one step with Gradle using the provided wrapper:

```
./gradlew bootRun -Pprofile=<PROFILE>
```

#### 2.3. Using Docker

A Dockerfile is provided which, once built, will run the above command in a container. By default it will use the 
`docker` profile. This can be changed by modifying the Dockerfile directly, or by changing the location of the
properties file at runtime, as shown below:

```
docker build . -n ocn-client
docker run ocn-client java -jar -Dspring.config.location=resources/main/application.<PROFILE>.properties lib/ocn-client-0.1.0-SNAPSHOT.jar
```

Note that building the docker image can take a few minutes.

To setup a local network, a compose file has been provided which runs a local Open Charging Network. This uses a test 
Ethereum blockchain with pre-funded accounts and the OCN Registry contract already deployed. Two clients are setup to 
start with, such that it is possible to test messages relayed between clients. Therefore, it is possible to register for
example, an eMSP with the first client, and a CPO with the second client.

In order to set this up, the OCN Registry contract repository needs to be cloned:

```
cd ..
git clone git@bitbucket.org:shareandcharge/ocn-registry.git
```

Following this, return to the OCN client directory (which should be under the same parent directory as the registry 
repo) and start the network:

```
cd ocn-client
docker-compose up
```

The first build may take a while (as above) but subsequent `docker-compose up` commands will be much faster. The 
following information is relevant to using this local network:

- OCN client 1 address: `http://localhost:8080`
- OCN client 2 address: `http://localhost:8081`
- Both Admin APIs use the same key, printend on startup: `randomkey`
- OCN Registry address: `0x345cA3e014Aaf5dcA488057592ee47305D9B3e10`
- OCN Registry owner: `0x627306090abaB3A6e1400e9345bC60c78a8BEf57`
- Ethereum blockchain JSON RPC address: `http://localhost:8544`
- Ethereum blockchain JSON RPC address from within container network: `http://172.16.238.10:8544`
- HD Wallet Mnemonic: `candy maple cake sugar pudding cream honey rich smooth crumble sweet treat`

### Using the OCN Client

Once the client is built and running, test that it is working with the following request:

```
curl localhost:8080/health
```

You should see a 200 OK response.

For further usage documentation, consult the [API Documentation](https://shareandcharge.bitbucket.io).

### Generating new API documentation

Documentation is generated automatically on build. The asciidoc template can be found in 
`src/docs/asciidoc/index.adoc` and the output in `build/asciidoc/html5/index.html`.