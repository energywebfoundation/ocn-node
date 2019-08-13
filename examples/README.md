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

In this tutorial, we will create an OCPI 2.2 connection with the OCN Client at [http:localhost:8080](http://localhost:8080). The CPO in our
demo has already registered to the other OCN Client at [http://localhost:8081](http://localhost:8081), which gives us
a chance to make requests across the network.

### 1. Adding an entry to the OCN Registry

Before we create a connection to an OCN client, we must enter our party into the OCN Registry to become visible on the 
network. To do this, we must sign a transaction which states our party's country code, party ID and client info of
the OCN client we will connect to. The OCN Client info that we need is its Ethereum address and base url.

Provided in the Postman collection is a request `GET Client Info` under the OCN directory. Note that there is no
authorization needed to make this request. The response should be the following:

```json
{
    "url": "http://localhost:8080",
    "address": "<ETHEREUM_ADDRESS>"
}
```

You might recall seeing the same information printed to stdout when running the local OCN. The above request can be used
when registering yourself to a test or production OCN environment. In our local development case, we can skip this 
manual step and use a script provided in the `ocn-demo` repository to save us some time. 

```
cd ocn-demo
npm run register-msp
```

You should see the following output: 

```
EMSP [DE MSP] has registered to the OCN on client http://localhost:8080 using wallet with address <ETHEREUM_ADDRESS>
```

Your EMSP wallet was generated randomly and has already been discarded. Fortunately, you won't need it again for this
tutorial. Should you wish to move to a different OCN Client (which you might want to in production), you would 
have to update your listing in the OCN Registry using the same wallet.

The script uses the above client info request under the hood to make sure that the data we are listing in the registry
is correct. You may inspect the rest of the script (located at `ocn-demo/scripts/register.js`) to see how the 
transaction was signed and sent to the network. 

### 2. Generating a CREDENTIALS_TOKEN_A

Now that we have listed ourselves in the OCN Registry smart contract, we need to create a connection with our OCN 
Client.

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
previous token is now discarded and will not be used again, so make sure to save this new token.

In addition, you should see that there were two requests made to the EMSP server. This shows that the OCN client has
requested and stored your OCPI module endpoints for future use.

This now completes the registration to the OCN client.

### 5. Making OCPI requests to the CPO

Now that we have registered to our OCN client, we can send requests to the CPO. In this request, we wish to fetch a 
list of the CPO's locations (i.e. charging stations under OCPI terminology). To do so, navigate to the `GET locations
list` request in the locations directory of the Postman collection. Substitute the `CREDENTIALS_TOKEN_C` in the
Authorization header and make the request.

The result should look like the following:
```
{
    "status_code": 1000,
    "data": [
                    {
                        "country_code": "DE",
                        "party_id": "MSP",
                        "id": "LOC1",
                        "type": "ON_STREET",
                        "address": "somestreet 1",
                        "city": "Essen",
                        "country": "DEU",
                        "coordinates": {
                            "latitude": "52.232",
                            "longitude": "0.809"
                        },
                        "evses": [
                            {
                                "uid": "1234",
                                "status": "AVAILABLE",
                                "connectors": [
                                    {
                                        "id": "1",
                                        "standard": "IEC_62196_T2",
                                        "format": "SOCKET",
                                        "power_type": "AC_3_PHASE",
                                        "max_voltage": 400,
                                        "max_amperage": 32,
                                        "tariff_ids": [
                                            "xxx-123"
                                        ],
                                        "last_updated": "2019-08-13T14:44:25.561Z"
                                    }
                                ],
                                "last_updated": "2019-08-13T14:44:25.561Z"
                            },
                            {
                                "uid": "4567",
                                "status": "RESERVED",
                                "connectors": [
                                    {
                                        "id": "1",
                                        "standard": "IEC_62196_T2",
                                        "format": "SOCKET",
                                        "power_type": "AC_3_PHASE",
                                        "max_voltage": 400,
                                        "max_amperage": 32,
                                        "tariff_ids": [
                                            "xyz-456"
                                        ],
                                        "last_updated": "2019-08-13T14:44:25.561Z"
                                    }
                                ],
                                "last_updated": "2019-08-13T14:44:25.561Z"
                            }
                        ],
                        "last_updated": "2019-08-13T14:44:25.561Z"
                    }
                ],
    "timestamp": "2019-08-13T15:25:24.435Z"
}
```

We see that the request was successfully processed, returning an array of a single location. The OCPI location data
type follows a hierarchy of `location` -> `evse` -> `connector`. We can make also make requests that fetch a single
location, or a specific EVSE or connector. Take a look at the other requests in the locations directory to see how they
work. 

Notice how on the Connector object there is a `tariff_ids` array. What does this mean? 

Let's find out. Navigate to the tariffs directory and send the `GET tariffs` request. This is an example of a dependency
between OCPI modules. However, all OCPI modules aside from `credentials` are optional. It is up to the EMSP/CPO (or any 
other role) to implement the modules themselves. Therefore if we try to make, for instance, a `sessions` request to the 
CPO, we might receive a message telling us that the CPO has not implemented the module (yet). 

That marks the end of this tutorial. More examples and use cases will be added to this tutorial in the future, but for 
now this should be enough to get started on creating an OCPI 2.2 platform that is ready to join the Open Charging Network.

The complete OCPI documentation can be found here: [https://github.com/ocpi/ocpi/tree/develop](https://github.com/ocpi/ocpi/tree/develop) 
(version 2.2 is contained in the develop branch).