# Open Charging Network Node

The Open Charging Network (OCN) node with Open Charge Point Interface (OCPI) v2.2 API. 

This is a community project, aimed at incorporating and building on the open OCPI standard. As with OCPI, contributions 
are welcome in the form of comments, pull requests and raised issues. Visit our 
[issue tracker](https://bitbucket.org/shareandcharge/ocn-node/issues) for an overview of current and past issues. 
Questions may also be asked on [Stack Overflow](https://stackoverflow.com/questions/tagged/shareandcharge), or in the 
[Gitter community](https://gitter.im/shareandcharge/community). 

Before contributing to the source code, please take the time to read over the 
[Developer Certificate of Origin](https://developercertificate.org/). For more information, see our 
[contributing guidelines](https://shareandcharge.atlassian.net/wiki/spaces/OCN/pages/360611849/Contributing+to+the+Open+Charging+Network).

## The Open Charging Network

The OCN is a decentralized eRoaming hub. To participate in the OCN, a node must be used to broker OCPI requests
(e.g. start/stop charging requests, POI data retrieval) between parties. A node can be set up and run by anyone, however 
to connect to a node, two steps are needed: 

1. A registration token (so-called Token A in OCPI terminology) must be generated for the prospective platform by the 
node administrator.
2. The platform must register themselves in the [OCN Registry](https://bitbucket.org/shareandcharge/ocn-registry), 
stating that they are using that particular node.

Once a registration token is obtained and the platform is listed in the registry, the OCPI credentials handshake with 
the OCN Node can be initiated, providing access to all OCPI modules and interfaces used for peer-to-peer 
communication<sup>1</sup>. When a counter-party is found (either offline or via the registry), requests are sent to 
them via the sender's OCN Node<sup>2</sup>.

For more information about the OCN, check out the [wiki](https://bitbucket.org/shareandcharge/ocn-node/wiki/).

<sup>1</sup> The HubClientInfo module will be added in a future release.\
<sup>2</sup> Sending a request does not guarantee its delivery. Using the _OcnRules_ module, parties are able to whitelist
and blacklist counter-parties. See the subsequent HTTP API documentation for more on this custom module. 

## HTTP API Documentation

The [HTTP API Documentation](https://shareandcharge.bitbucket.io) for the OCN Node describes endpoints which can be used 
by administrators and users (OCPI parties). Outside of the full OCPI v2.2 API, OCN Nodes provide additional features,
such as the custom OCPI module, _OcnRules_, as well as ways for admins to restrict use and users to query the OCN Registry.

## Dependencies

The OCN Node is built with Kotlin, targeting the JVM. [OpenJDK 8](https://openjdk.java.net/install/index.html) or higher 
is needed to build and run the project. Additionally, the node can be run in [Docker](https://docs.docker.com/install/) 
with the provided Dockerfile, however the build step takes place on the host rather than inside a container.

The choice of operating system is up to the administrator. By and large, the OCN Node has been developed and run on
Unix-like operating systems, particularly Ubuntu. There is currently no guarantee that it will work on other 
operating systems.   


## Tutorial: Running your own Local Open Charging Network

Before running a node and connecting it to a local, test or prod environment, it is recommended to first become 
acquainted with how the network operates. The provided `docker-compose` file spins up a local environment with the OCN
Registry and two OCN Nodes pre-configured. A [tutorial](./examples) has been provided to guide administrators and users 
of an OCN Node alike through various use case examples.  To complete this tutorial it is necessary to install
[Docker Compose](https://docs.docker.com/compose/install/) in addition to the above dependencies.

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

means that parties receive module endpoints starting with `http://localhost:8080`, for 
example `http://localhost:8080/ocpi/2.2/sender/locations` (the locations module's sender interface).
 
Likewise, for a public node that accepts outside connections, the url might include the domain name, for example 
`https://server.example.com`. This would translate to `https://server.example.com/ocpi/2.2/sender/locations`. Be sure 
to include the protocol so that connected platforms can correctly parse the endpoints provided.

On startup, the OCN Node will ensure that the public URL set by the administrator is reachable. Additionally, there is
different behaviour within this check depending on which mode the client is running in. See section 1.5 for more details.

#### 1.2. Connecting to a Database

The `dev` properties connects the node to an in-memory database, which will not persist data across node restarts.
If running the node in a test or production environment with Postgres installed, copy from the `prod` properties file 
instead:

```
cp application.prod.properties application.prod-edit.properties
```

The node may also be connected to a different database. This requires installing the relevant database driver as 
an `ocn-node` dependency via gradle and modifying application properties accordingly, in addition to running the 
database server itself. 

#### 1.3. Configuring the Network

The network on which any OCN Node is running on depends purely on the OCN Registry smart contract it is connected to,
defined by the blockchain node and registry contract address configured. These configuration properties have the prefix 
`ocn.node.web3`. 

Currently there are two environments: 
[test](https://shareandcharge.atlassian.net/wiki/spaces/OCN/pages/409206816/Public+Test+Network) and
[production](https://shareandcharge.atlassian.net/wiki/spaces/OCN/pages/409305103/Production+Network).

The pre-configured properties files point to each of these respectively (`dev` to test, `prod` to production).

#### 1.4. Setting the Admin API key

The Admin API allows, for example, generating new OCPI tokens (`CREDENTIALS_TOKEN_A`) for planned platforms. An API
key can be set in the application's properties, else a new one will be generated on restart:

```
ocn.node.apikey = randomkey
```

The API key will be printed on node start, be it generated or user-specified. Consult the 
[API documentation](https://shareandcharge.bitbucket.io) for more information on how to use the Admin API. 

#### 1.5 Setting the runtime mode

The client can be run in "dev" or "prod" mode. By default, prod mode is in effect, which will check to see if the
provided public client URL is reachable over HTTPS and is accessible to the outside world. When running in dev mode 
(by setting `ocn.client.dev = true`), the client will allow insecure HTTP connections made over localhost.

#### 1.6 Enabling message signing

A feature of the Open Charging Network is it's security. To ensure that requests sent over the OCN are delivered to 
their recipient as intended, requests are signed by the sender and responses by the receiver. In some particular cases, 
an OCN Node may need to rewrite entries in the sender's original request (such as when providing response URLs in 
commands requests). The message signing functionality allows for this, as the signature of a modified request must also 
reference any values that have been changed. For more details on how to sign and verify messages, check out the 
[OCN Notary](https://bitbucket.org/shareandcharge/ocn-notary).

By default message signing verification is on. To toggle this feature on/off, set `ocn.node.signatures` to true (on) or 
false (off). 

In practice, this means that a connected platform will need to send requests including a valid `OCN-Signature` header,
signed by the private key which they have used to list themselves in the OCN Registry. Likewise the recipient will
need to sign their response, and include it in the body. As the signature for responses can potentially be large 
(depending on the amount of data signed), it was decided that they should be placed in the response body. 

Be aware that even if an OCN Node does not require message signing, a recipient may still reject the request if it's
missing an `OCN-Signature` (either on their own or through the _OcnRules_ module). Additionally, the OCN Node will 
verify signatures if they are present in a a request/response, regardless of this setting. 

#### 1.7 Providing a private key

An Etheruem public-private key-pair is used to identify the node on the network. Commonly, these key-pairs are stored 
in a "wallet". There are a number of ways that a wallet can be created, for example with [Metamask](https://metamask.io/).

Once a private key has been created, set the node to use it:

```
ocn.node.privatekey = 0x1c3e5453c0f9aa74a8eb0216310b2b013f017813a648fce364bf41dbc0b37647
```

Note that in dev mode, a private key will be set if omitted. As this key is hardcoded and used in a lot of examples
elsewhere (like above), it is advised to change it if targeting the public test environment. In prod mode, the node
will refuse to start without a private key.  

### 2. Listing the Node in the OCN Registry

A Node must be listed in the registry for it to be usable on the network. This can be achieved by installing
the OCN Registry CLI. Either clone the [OCN-Registry](https://bitbucket.org/shareandcharge/ocn-registry) repository
and follow the instructions in the README, or install the NPM package:

```
npm i -g @shareandcharge/ocn-registry
```

Then, add your domain name using the private key as set in the configuration (note that the wallet key must be funded
and the correct network chosen using the `-n` flag:
```
ocn-registry set-node https://server.example.com -n prod -s 0x1c3e5453c0f9aa74a8eb0216310b2b013f017813a648fce364bf41dbc0b37647
```

If successful the node is now available for prospective platforms to link themselves to in the OCN Registry.

### 3. Running the Node

There are multiple ways to run an OCN Node. 

Note: make sure you are in the root of the repository first.

#### 3.1. Building and Executing a JAR file

To build the node with the desired profile (relating to the `application.<PROFILE>.properties` file), for example a
`local` profile, run the following:

```
./gradlew -Pprofile=local build -x test
```

Once built, the packaged node can be run using:
```
java -jar ./build/libs/ocn-node-1.0.0.jar
```

Alternatively, a different profile can be selected at runtime, e.g.:
```
java -jar -Dspring.config.location=/path/to/application.prod.properties ./build/libs/ocn-node-1.0.0.jar
```

#### 3.2. Using the Gradle Wrapper

Especially helpful for development, the node can quickly be run in one step with Gradle using the provided wrapper:

```
./gradlew -Pprofile=local bootRun
```

#### 3.3. Using Docker

A Dockerfile is provided which, once built, will run the above command in a container. Firstly, build the OCN Node with
gradle locally, then build the Docker image with a tag flag so that we can identify it later:

```
./gradlew -Pprofile=local build -x test
docker build -t ocn-node .
```

Once built, the node can be run and accessed by exposing the application server's `8080` port within the container to 
the outside environment:
```
docker run -p 8080:8080 ocn-node 
```

### 4. Operating the OCN Node

Once the node is running, test that it is working with the following request:

```
curl localhost:8080/health
```

You should see a 200 OK response.

For further usage documentation, consult the [API Documentation](https://shareandcharge.bitbucket.io).

## Development

Gradle tasks are configured in `build.gradle.kts` using the Kotlin DSL.

### Run unit tests

```
./gradlew unitTest
```

### Run integration tests

Integration tests depend on `ganache-cli`, a local development blockchain, which is installed using NPM. In one terminal 
window, run the following task, which will attempt to install ganache if not already present and then run it:
```
./gradlew ganache
```

Then, run the tests:
```
./gradlew integrationTest
```

### Developing against Ganache

This is helpful for developing without having to worry about funding and managing Ethereum keypairs. With the above
`ganache` task running in the background, the OCN Node can be configured using the following properties:

```
ocn.node.web3.provider = http://localhost:8544
ocn.node.web3.contracts.registry = 0x345ca3e014aaf5dca488057592ee47305d9b3e10
```

### Generating new API documentation

Documentation is generated automatically on build. The asciidoc template can be found in 
`src/docs/asciidoc/index.adoc` and the output in `build/asciidoc/html5/index.html`.