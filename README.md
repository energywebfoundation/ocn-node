# Open Charging Network Client

The Open Charging Network (OCN) Client with OCPI 2.2 connection.

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
in the application properties file. With the wallet created, we can now configure the application properties for your 
desired environment, e.g. `local`:

```
cd src/main/resources
cp application.example.properties application.local.properties
vi application.local properties
```

Be sure to also change the other relevant `ocn.client.web3` configuration properties to match your Ethereum 
environment (i.e. if on Tobalaba, point to a Tobababa HTTP provider and use the address of the Registry contract
which has been deployed on Tobalaba).

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