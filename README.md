# Open Charging Network Client

The Open Charging Network (OCN) Client with OCPI 2.2 connection. 

**This software is in alpha**. Starting from end of July, a testing period will be run to aid and inform development.
Additionally, contributions are always welcome in the form of pull requests and issues.

## API Documentation

See [Open Charging Network Client Documentation](https://shareandcharge.bitbucket.io).

## Running a Client Locally

Clone the repository:

```
git clone git@bitbucket.org:shareandcharge/ocn-client.git
cd ocn-client
```

You will need to create an Ethereum-compliant wallet before you can start the client 
(for instance, with [web3j](https://github.com/web3j/web3j/releases)):

```
web3j wallet create
```

Follow the instructions to create the wallet, making note of the filepath and password. They will need to be referenced
in the application properties file.

With the wallet created, we can now configure the application properties for your desired environment, e.g. `local`:

```
cd src/main/resources
cp application.dev.properties application.local.properties
vi application.local properties
```

The dev properties connects the client to an in-memory database, which will not persist data across client restarts.
If running the client in a test or production environment with Postgres installed, copy from the `psql` properties file 
instead:

```
cd application.psql.properties application.local.properties
```

The client may also be connected to a different database. This requires installing the relevant database driver as 
an `ocn-client` dependency via gradle, in addition to the database server itself.

Be sure to also change the other relevant `ocn.client.web3` configuration properties to match your Ethereum 
environment. The provided configuration points the client to a HTTP provider on localhost and to the Registry smart 
contract on Tobalaba (`0xD7Fa6B5CD7333F02EE443BfeA81911e3d9724810`).

Note that for development on a local Ethereum chain (using e.g. `ganache`), you need not create a wallet. If the 
client cannot find the provided wallet file, it will default to using a pre-defined keypair. As this is pre-defined,
it **should not be used on a public chain**.

Next, build the client with the desired profile:

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