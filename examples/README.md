# Open Charge Network (OCN) Tutorial

The following examples will guide you through administration of an OCN Node, as well as basic Open Charge Point 
Interface implementation guidelines to aid with development of eMobility Service Provider (eMSP) or Charge Point 
Operator (CPO) backoffices.

## Setup

To follow and run the examples, there are a few dependencies that must first be met:

#### 1. Run a Local Open Charging Network

You will first need to install [Docker](https://docs.docker.com/install/) and [docker-compose](https://docs.docker.com/compose/install/) 
on your chosen platform to run the local network.

Next, ensure that the OCN Registry and Node repositories are cloned under the same parent directory:

```
git clone git@bitbucket.org:shareandcharge/ocn-registry.git
git clone git@bitbucket.org:shareandcharge/ocn-node.git
```

The directory structure should look like so:

```
- ocn/
    | - ocn-registry/
    | - ocn-node/
```

Then change directory to `ocn-node` and run the network: 

```
cd ocn-node
docker-compose up
```

The first build will take a few minutes but subsequent `docker-compose up` commands will be much faster. Whilst the Docker
images are being built, you may look over the details of the network below, which may or may not mean anything depending 
on prior knowledge of Ethereum blockchain technology. Don't worry if they do not, as they will be explained in the following
section when we walk through the example requests.

- OCN Registry smart contract deployed on a local development Ethereum blockchain (ganache)
    - address: `0x345cA3e014Aaf5dcA488057592ee47305D9B3e10`
    - owner: `0x627306090abaB3A6e1400e9345bC60c78a8BEf57`
- A Wallet containing 20 addresses each pre-funded with 100 ETH
    - mnemonic: `candy maple cake sugar pudding cream honey rich smooth crumble sweet treat`
- Reachable blockchain JSON RPC API
    - provider: `http://localhost:8544`
- Two OCN Nodes connected to the same OCN Registry as above
    - node 1 address: `http://localhost:8080`
    - node 2 address: `http://localhost:8081`
    - admin API keys for both nodes: `randomkey`

Once the images are built and the containers are running, the following will show in stdout:

```
ocn-node-1       | [...] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
ocn-node-1       | [...] s.o.node.ApplicationKt                   : Started ApplicationKt in 20.76 seconds (JVM running for 22.842)
ocn-node-2       | [...] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8081 (http) with context path ''
ocn-node-2       | [...] s.o.node.ApplicationKt                   : Started ApplicationKt in 20.448 seconds (JVM running for 22.829)
```

You will also see the results of a request, for example:

```
ocn-node-2         | [...] o.s.web.servlet.DispatcherServlet        : GET "/health", parameters={}
ocn-node-2         | [...] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped to snc.openchargingnetwork.node.controllers.HealthController#getHealth()
ocn-node-2         | [...] m.m.a.RequestResponseBodyMethodProcessor : Using 'text/plain', given [*/*] and supported [text/plain, */*, text/plain, */*, application/json, application/*+json, application/json, application/*+json]
ocn-node-2         | [...] m.m.a.RequestResponseBodyMethodProcessor : Writing ["OK"]
ocn-node-2         | [...] o.s.web.servlet.DispatcherServlet        : Completed 200 OK
```

The node checks its own health endpoint on start to make sure it is configured correctly. We can also manually ensure 
that both nodes are running, like so:

```
curl localhost:8080/health
curl localhost:8081/health
```

Now leave the network running and open a new tab in your terminal.

#### 2. Clone the OCN Demo Repository and run the Mock servers 

The OCN Demo repository contains mock eMSP and CPO backoffices, as well as a script which will allow us to register 
to the OCN Registry. Clone it by returning to the parent directory:

```
cd ..
git clone git@bitbucket.org:shareandcharge/ocn-demo.git
cd ocn-demo
npm install
```

The demo contains two Charge Point Operators (CPOs) and one eMobility Service Provider (EMSP). Starting the demo will 
start the respective backends and register the CPOs to the Open Charging Network that we have running in the background:

```
npm start
``` 

Observe the requests that are made to ganache, the ocn-node and the CPO server. This gives a quick overview of how
the OCN registration process using OCPI and the OCN Registry looks. For those familiar with OCPI, you will see the
CPOs receiving versions and version detail requests, as the OCN node processes the credentials registration.

Leave the servers running in the background, alongside the network. The next step is to install Postman so that we can 
register our EMSP to the network.

#### 3. Install Postman and Import Collection

In order to visualize HTTP API requests, we can use [Postman](https://www.getpostman.com/). Once installed, simply
import the JSON collection and environment files provided in this directory. You will need to change your environment
to the provided "OCN Node" environment that you just imported, by selecting it from the dropdown menu in the top right
corner of Postman. This will allow you to easily change common variables across all your requests in the future.


## Tutorial

In this tutorial, we will create an OCPI 2.2 connection with the OCN Node at [http:localhost:8080](http://localhost:8080). If you completed the
above step, there should be one CPO already registered with our OCN Node and another at [http://localhost:8081](http://localhost:8081), which gives us
a chance to make different requests across the network.

### 1. Adding an entry to the OCN Registry

Before we create a connection to an OCN Node, we must enter our party into the OCN Registry to become visible on the 
network. To do this, we must sign a transaction which states our party's country code and party IDm as well as the
public information of the OCN Node we will connect to. The OCN Node information that we need is its Ethereum address and 
base url.

Provided in the Postman collection is a request `GET Node Info` under the OCN directory. Note that there is no
authorization needed to make this request and the variable `{{NODE_URL}}` has been configured to `http://localhost:8080`
within the "OCN Node" environment that you imported. To change environment variables, click the eye symbol button next
to the dropdown menu where you selected the environment. The response should be the following:

```json
{
    "url": "http://localhost:8080",
    "address": "<ETHEREUM_ADDRESS>"
}
```

You might recall briefly seeing the same information printed to stdout when running the local OCN. The above request can 
be used when registering yourself to a test or production OCN environment. In our local development case, we can skip this 
manual step and use a script provided in the `ocn-demo` repository to save us some time. 

```
cd ocn-demo
npm run register-msp
```

You should see the following output: 

```
EMSP [DE MSP] has registered to the OCN on node http://localhost:8080 using wallet with address <ETHEREUM_ADDRESS>
```

Your EMSP wallet was generated randomly and has already been discarded. Fortunately, you won't need it again for this
tutorial. Should you wish to move to a different OCN Node (which you might want to in production), you would 
have to update your listing in the OCN Registry using the same wallet.

The script uses the above node info request under the hood to make sure that the data we are listing in the registry
is correct. You may inspect the rest of the script (located at `ocn-demo/scripts/register.js`) to see how the 
transaction was signed and sent to the network. 

### 2. Generating a CREDENTIALS_TOKEN_A

Now that we have listed ourselves in the OCN Registry smart contract, we need to create a connection with our OCN 
Node.

In order to connect to an OCN Node, the administrator first must generate a token to be used in the OCPI credentials
registration flow. This, described as `CREDENTIALS_TOKEN_A`, will be used to obtain information about the OCN Node, 
e.g. the version of OCPI it is using and the modules it incorporates.

In Postman, simply go to the `GET Generate Registration Token` request in the Admin directory of the OCN Node 
collection and hit Send to make the request. The authorization token (`{{ADMIN_API_KEY`}}) has already been declared in 
the "OCN Node" environment. 

You should see the following response:
```
{
    "token": {{CREDENTIALS_TOKEN_A}},
    "versions": "http://localhost:8080/ocpi/versions"
}
```

Taking the provided token, we can now set our environment variable, using the eye symbol button in Postman, as 
described before.

### 2. Request Versions and Version Details

Our EMSP backend runs on OCPI 2.2. As such we would like to establish a connection to the OCN Node using the same
OCPI version. In Postman, navigate to the Versions directory and send the `GET versions` request.

This is our first chance to see the OCPI JSON response format, containing the OCPI status code of the response, the
timestamp, and (optionally) any data returned. If the request is made with an incorrect token, the HTTP status code
will be 401 rather than 200, and the `data` field will be (optionally) replaced with `status_message`. 

The data field should provide an array of supported versions and a url that will provide information regarding how to 
use this version.

Do the same for the next request in the directory, for retrieving version details. The response will contain an array
of endpoints supported by the node. The one we are most interested in for now is the `credentials` module:

```json
{
    "identifier": "credentials",
    "role": "SENDER",
    "url": "http://localhost:8080/ocpi/2.2/credentials"
}
```

### 3. Registering EMSP credentials

Now that we know where to send our credentials, we can open the `POST credentials` request in the credentials directory.
This request contains a JSON body of our own credentials to be sent to the OCN Node. This includes the token that the 
OCN Node should use to authorize itself on *our* server, should it need to forward a request to us from a CPO (i.e. if 
the CPO is pushing session or location updates to us). We also provide a url to  our versions endpoint, such that the 
OCN Node can go through the same process we just did, in finding a common OCPI version and obtaining a list of the 
endpoints we have. Lastly, we describe the roles that we employ. Notice how this is an array. OCPI 2.2 adds the ability 
for a platform operating multiple roles to communicate on a single OCPI connection. Therefore a platform that is both an 
EMSP and CPO needs not register twice. 

```json
{
    "status_code": 1000,
    "data": {
        "token": "ef4c3b29-5679-4bb9-8a59-4c53fc3dacef",
        "url": "http://localhost:8080/ocpi/versions",
        "roles": [
            {
                "role": "HUB",
                "business_details": {
                    "name": "Open Charging Network Node"
                },
                "party_id": "OCN",
                "country_code": "DE"
            }
        ]
    },
    "timestamp": "2020-01-15T09:56:56.547Z"
}
```

Once sent, you should see the OCN Node's credentials returned to you. There is a new token in the body: this is what's 
known as token C and will be used to authorize any subsequent OCPI requests you make to your OCN Node. 
The previous token is now discarded and will not be used again, so make sure to save this new token under the 
environment variable `CREDENTIALS_TOKEN_C`.

In addition, you should see that there were two requests made to the EMSP server in the logs of the OCN demo. This 
shows that the OCN Node has requested and stored your OCPI module endpoints for future use.

This now completes the registration to the OCN Node.

### 4. Making OCPI requests to a CPO

#### Requesting location data

Now that we have registered to our OCN Node, we can send requests to one of the registered CPOs on the OCN. In this 
request, we wish to fetch a list of the CPO's locations (i.e. charging stations under OCPI terminology). To do so, 
send the `GET locations list` request in the locations directory of the Postman collection.

The result should look like the following:
```json
{
    "status_code": 1000,
    "data": [
        {
            "country_code": "DE",
            "party_id": "CPO",
            "id": "LOC1",
            "publish": true,
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

#### A note on headers

The headers prefixed with `OCPI-` describe the desired routing of the message. The `OCPI-from-country-code` and 
`OCPI-from-party-id` describe the OCPI party on the platform which is making the request (in our case we only have one
party per platform, but it could be the case that a CPO and EMSP role share the same platform connection with an OCN
node). Likewise, the `OCPI-to-country-code` and `OCPI-to-party-id` headers describe the recipient of the request. In
our case we are making requests to a CPO with country code `DE` and party ID `CPO`. This CPO is registered to the same
OCN Node as our EMSP, but what if we want to contact a "remote" party connected to a different OCN Node? We can
try this out by changing the request headers to the following:

```
OCPI-to-country-code: NL
OCPI-to-party-id: CPX
```

Note also the `X-Request-ID` and `X-Correlation-ID` headers. They don't play a role in our demonstration (both being
set to `"1"` for all requests, but in production it is strongly advised to generate unique IDs (uuid version 4 preferred)
for all requests, in order to help with any potential debugging. The request ID is unique for every request: in 
forwarding such a request: an OCN Node will actually set a new request ID when forwarding a message. The correlation
ID meanwhile, is unique for every request-response: an OCN Node will never change the correlation ID. 

#### OCPI module dependencies and implementations

Let's check out the other request types we can make now. Notice how on the Connector object there is a `tariff_ids` array. 
What does this mean? 

Navigate to the tariffs directory and send the `GET tariffs` request. You should see in the response's body an array of
tariffs provided by the CPO, with IDs matching those found on the Connector object. This is an example of a dependency
between OCPI modules. 

However, all OCPI modules aside from `credentials` are optional (for the purpose of the demo the
EMSP and CPOs have not implemented the `credentials` interface, but it is important to do so, as the OCN Node could
need to update its credentials on your system). It is up to the EMSP/CPO (or any other role) to implement the modules
themselves. Therefore if we try to make, for instance, a `sessions` request to the CPO, we might receive a message 
telling us that the CPO has not implemented the module (yet).

#### More complex requests

Our EMSP has actually implemented two OCPI modules: `commands` and `cdrs`. The first of which is the `SENDER` interface
which allows the CPO to asynchronously notify the EMSP of a command result, i.e. a session has been started by a charge
point. The second allows the CPO to push Charge Detail Records (CDRs), containing the final price of a charging session, 
to the EMSP. This reduces the load on the CPO backend as the EMSP doesn't need to poll for new CDRs.

We can see this first hand by sending the requests in the commands directory. The first, `POST START_SESSION`, will 
send a start session request on behalf of a driver on the EMSP system. For this request, we can also monitor the output
of our demo servers. The initial response from our Postman request contains only an acknowledgement of the request.
After 5 seconds the CPO will send the async command result (the response from the charge point):

```
CPO [DE CPO] sending async STOP_SESSION response
EMSP [DE MSP] received async command response: {"result":"ACCEPTED"}
EMSP [DE MSP] -- POST /ocpi/emsp/2.2/commands/STOP_SESSION/2 200 59 - 0.734 ms
```

Likewise, when we make a `POST STOP_SESSION` request, we see the following a further 5 seconds after the async response
has been sent to the EMSP:

```
CPO [DE CPO] sending cdr after session end
EMSP [DE MSP] -- POST /ocpi/emsp/2.2/cdrs 200 59 - 0.487 ms
EMSP [DE MSP] -- GET /ocpi/emsp/2.2/cdrs/1 200 1124 - 0.784 ms
CPO [DE CPO] acknowledges cdr correctly stored on EMSP system
```

In this case, the CPO has sent a POST `cdrs` request to the EMSP, the response of which will contain a `Location` header
set by the EMSP describing where the CPO can find this charge detail record on the EMSP system. The CPO can then make
a GET request to this location to verify that the CDR was stored correctly by the EMSP. 

#### Next steps

That marks the end of this tutorial. More examples and use cases will be added to this tutorial in the future, but for 
now this should be enough to get started on creating an OCPI 2.2 platform that is ready to join the Open Charging Network.

The complete OCPI documentation can be found here: [https://github.com/ocpi/ocpi/tree/develop](https://github.com/ocpi/ocpi/tree/develop) 
(version 2.2 is contained in the develop branch).
