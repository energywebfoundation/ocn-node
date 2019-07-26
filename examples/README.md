# Open Charge Network (OCN) Tutorial

The following examples will guide you through administration of an OCN Client, as well as basic Open Charge Point 
Interface implementation guidelines to aid with development of eMobility Service Provider (eMSP) or Charge Point 
Operator (CPO) backoffices.

## Setup

To follow and run the examples, there are a few dependencies that must first be met:

#### 1. Run a Local Open Charging Network

You will first need to install [Docker](https://docs.docker.com/install/) and [docker-compose](https://docs.docker.com/compose/install/) 
on your chosen platform to run the local network.

Next, ensure that the OCN Registry and Client repositories are cloned under the same parent directory:

```
git clone git@bitbucket.org:shareandcharge/ocn-registry.git
git clone git@bitbucket.org:shareandcharge/ocn-client.git
```

The directory structure should look like so:

```
- ocn/
    | - ocn-registry/
    | - ocn-client/
```

Then change directory to `ocn-client` (using the `develop` branch) and run the network: 

```
cd ocn-client
git checkout develop
docker-compose up
```

The first build will take a few minutes but subsequent docker-compose up commands will be much faster. Whilst the Docker
images are being built, you may look over the details of the network below, which may or may not mean anything depending 
on prior knowledge of Ethereum blockchain technology. Don't worry if they do not, as they will be explained in the following
section when we walk through the example requests.

- OCN Registry smart contract deployed on a development Ethereum blockchain (ganache)
    - address: `0x345cA3e014Aaf5dcA488057592ee47305D9B3e10`
    - owner: `0x627306090abaB3A6e1400e9345bC60c78a8BEf57`
- A Wallet containing 20 addresses each pre-funded with 100 ETH
    - mnemonic: `candy maple cake sugar pudding cream honey rich smooth crumble sweet treat`
- Reachable blockchain JSON RPC API
    - provider: `http://localhost:8544`
- Two OCN Clients connected to the same OCN Registry as above
    - client 1 address: `http://localhost:8080`
    - client 2 address: `http://localhost:8081`
    - admin API keys for both clients: `randomkey`

Once the images are built and the containers are running, the following will show in stdout:

```
ocn-client-1       | [...] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
ocn-client-1       | [...] s.o.client.ApplicationKt                 : Started ApplicationKt in 20.76 seconds (JVM running for 22.842)
ocn-client-2       | [...] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8081 (http) with context path ''
ocn-client-2       | [...] s.o.client.ApplicationKt                 : Started ApplicationKt in 20.448 seconds (JVM running for 22.829)
```

Finally, go ahead and request the health of the two clients to make sure they are running:

```
curl localhost:8080/health
curl localhost:8081/health
```

Both requests should return 200 OK responses. 

Leave the network running and open a new tab in your terminal.

#### 2. Clone the OCN Demo Repository and run the Mock servers 

The OCN Demo repository should be cloned, which contains mock eMSP and CPO backoffices, as well as a means of
registering to the OCN Registry. 

```
cd ..
git clone git@bitbucket.org:shareandcharge/ocn-demo.git
cd ocn-demo
npm install
```

Currently, only the demo only contains one Charge Point Operator (CPO) and one eMobility Service Provider (EMSP). 
Starting the demo will start the respective backends and register the CPO to the Open Charging Network that was
previously set up:

```
npm start
``` 

Observe the requests that are made to ganache, the ocn-client and the CPO server. This gives a quick overview of how
the OCN registration process using OCPI and the OCN Registry works.

Leave the servers running alongside the network. The next step is to install Postman so that we can register our EMSP
to the network.

#### 3. Install Postman and Import Collection

In order to visualize HTTP API requests, we can use [Postman](https://www.getpostman.com/). Once installed, simply
import the JSON collection file provided in this directory and you are ready to begin.


## Tutorial

As the communication across OCN clients is still experimental, we will for now only be sending requests to OCPI parties
connected to the same OCN client. Therefore, our EMSP will connect to the same OCN client as the CPO registered in
step 2 of the setup. 

### 1. Generating a CREDENTIALS_TOKEN_A

In order to connect to an OCN client, the administrator first must generate a token to be used in the OCPI credentials
registration flow. This, described as `CREDENTIALS_TOKEN_A`, will be used to obtain information about the OCN Client, 
e.g. the version of OCPI it is using and the modules it incorporates.

In Postman, simply go to the `GET Generate Registration Token` request in the Admin directory of the OCN Client 
collection and hit Send to make the request.

You should see the following response:
```
{
    "token": <CREDENTIALS_TOKEN_A>,
    "versions": "http://localhost:8080/ocpi/versions"
}
```

Make sure to keep this request tab open as we will need the provided data for subsequent requests.

### 2. Request Versions and Version Details

Our EMSP backend runs on OCPI 2.2. As such we would like to establish a connection to the OCN Client using the same
OCPI version. In Postman, navigate to the Versions directory and select the `GET versions` request. You will need to
modify this request slightly for it to work. In the Headers tab, edit the `Authorization` key, which currently has the 
value `Token CREDENTIALS_TOKEN_A`. Replace `CREDENTIALS_TOKEN_A` with the token received from the above request.

This is our first chance to see the OCPI JSON response format, containing the OCPI status code of the response, the
timestamp, and (optionally) any data returned. If the request is instead made with an incorrect token, the HTTP status code
will be 401 rather than 200, and the `data` field will be (optionally) replaced with `status_message`. 

The data field should provide an array of supported versions and a url that will provide information regarding how to 
use this version.

Do the same for the next request in the directory, for retrieving version details. The response will contain an array
of endpoints supported by the client. The one we are most interested in for now is the `credentials` module:

```
{
    "identifier": "credentials",
    "role": "SENDER",
    "url": "http://localhost:8080/ocpi/2.2/credentials"
}
```

### 3. Registering EMSP credentials

Now that we know where to send our credentials, we can open the `POST credentials` request in the credentials directory.
Again, we should change the `Authorization` header to our `TOKEN_A`. This request contains a JSON body, as we our
sending our own credentials to the OCN client. This includes the token that the OCN client should use to authorize
itself on *our* server, should it need to forward a request to us from a CPO (i.e. the CPO is pushing session or location
updates to us). For now, the test CPO backend does not do this, so it is not so important. We also provide a url to 
our versions endpoint, such that the OCN client can go through the same process we just did, in finding a common OCPI
version and obtaining a list of the endpoints we have. Lastly, we describe the roles that we employ. Notice how this is
an array. OCPI 2.2 adds the ability for a platform operating multiple roles to communicate on a single OCPI connection.
Therefore a platform that is both an EMSP and CPO needs not register twice. 

Go ahead and send the request once the proper `Authorization` header has been set. 

You should see the OCN client's credentials returned to you. There is a new token in the body: this is what's known as
`CREDENTIALS_TOKEN_C` and will allow you to authorize any subsequent OCPI requests you make to your OCN client. The 
previous token is now discarded and will not be used again.

In addition, you should see that there were two requests made to the EMSP server. This shows that the OCN client has
requested and stored your OCPI module endpoints for future use.

This now completes the registration to the OCN client.

### 4. Adding an entry to the OCN Registry

Though we have successfully registered to an OCN client, there is no way of an OCPI party connected to a different
client to contact us. To become *visible* on the network, we must register ourselves to the OCN Registry.

This step has no tutorial yet, and it is not essential if we only need to communicate with parties connected to our
OCN client. 

The CPO in our network has already fully registered on the network. You can see how this was achieved by reading [some
code](https://bitbucket.org/shareandcharge/ocn-demo/src/master/src/index.js)! Line 80-100 shows how it was done.

### 5. Making OCPI requests to the CPO

Coming soon...