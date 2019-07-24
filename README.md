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

### 1. Creating and Funding a Wallet

You will need to create an Ethereum-compliant wallet before you can start the client 
(for instance, with [web3j](https://github.com/web3j/web3j/releases)):

```
web3j wallet create
```

Follow the instructions to create the wallet, making note of the filepath and password. They will need to be referenced
in the application's configuration.

In this initial version of the OCN client, interaction with the decentralized registry (used for routing OCPI requests
between clients) is taken care of by the OCN client on which the OCPI platform is registered. For example, if a CPO
registers with an OCN client, in order to be reachable on the network, the client registers the CPO to the Registry
smart contract. Normally the CPO platform would need to manage a blockchain wallet, obtain funds and send a 
transaction to the smart contract to signal that they are registered with a particular OCN client. As this step 
requires additional work and is not the focus of this first test version, it has been delegated to the OCN client. 
In future releases, **registering to an OCN client and registering to the Registry smart contract will be decoupled**.

For the reasons outlined above, the wallet that is created will need to be funded. As the OCN client is configured
to use the Energy Web Foundation's Volta test network by default, the [Volta faucet](https://voltafaucet.energyweb.org/)
can be used to request funds.

### 2. Modifying Client Configuration

With the wallet created and funded, we can now configure the application properties for your desired environment, 
e.g. `local`:

```
cd src/main/resources
cp application.dev.properties application.local.properties
vi application.local properties
```

#### 2.1. Setting the Client Address

The field `ocn.client.url` describes the client's server address. If running a local network as part of development, 
this would be `http://localhost:8080`. If requesting endpoints from the OCPI `versions` module, the sender 
interface for the `locations` module would therefore be located at `http://localhost:8080/ocpi/2.2/sender/locations`. 
Likewise, for a public node on the test network, the url would be `https://client.example.com`. This would translate
to `https://client.example.com/ocpi/2.2/sender/locations`.

#### 2.2. Connecting to a Database

The dev properties connects the client to an in-memory database, which will not persist data across client restarts.
If running the client in a test or production environment with Postgres installed, copy from the `psql` properties file 
instead:

```
cd application.psql.properties application.local.properties
```

The client may also be connected to a different database. This requires installing the relevant database driver as 
an `ocn-client` dependency via gradle, in addition to the database server itself.

#### 2.3. Configuring the Network

The network on which any OCN client is running on depends purely on the Registry smart contract it is connected to.
These configuration properties belong to `ocn.client.web3`. Currently the OCN test environment exists on the Energy Web 
Foundation's Volta test network. The provided configuration points the client to a 
[remote Volta node](https://energyweb.atlassian.net/wiki/spaces/EWF/pages/703201459/Volta+Connecting+to+Remote+RPC) 
and to the Registry smart contract deployed on Volta (with address `0x668956FE2Eb6ED52C5a961b02bEEbAc8913A2731`).

Note that for development on a local Ethereum chain (using e.g. `ganache`), you need not create a wallet. If the 
client cannot find the provided wallet file, it will default to using a pre-defined keypair. As this is pre-defined,
it **should not be used on a public chain**.

### 3. Building and Running the Client

Next, build the client with the desired profile (relating to the `application.<ENV>.properties` file):

```
./gradlew -Pprofile=local build
```

Finally, run the compiled client:
```
java -jar ./build/libs/ocn-client-0.0.1-SNAPSHOT.jar
```

Once it is built and running, test that it is working with the following request:

```
curl localhost:8080/health
```

You should see a 200 OK response.

### Generating new API documentation

Documentation is generated automatically on build. The asciidoc template can be found in 
`src/docs/asciidoc/index.adoc` and the output in `build/asciidoc/html5/index.html`.