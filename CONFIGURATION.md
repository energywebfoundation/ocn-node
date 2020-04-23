# OCN Node Configuration

By default, a node will point to the Public Test Environment of the Open Charging Network. This
network allows parties and node operators to play around with and develop their implementation before
joining the production OCN environment.

An OCN Node is configured using properties. They can be applied in [several ways](https://docs.spring.io/spring-boot/docs/1.2.2.RELEASE/reference/html/boot-features-external-config.html)
but generally speaking, are either found in a `.properties` file or as command line arguments.

Three example properties files have been provided as a starting point. By default, the OCN Node runs using
`dev` properties. Such example properties can be found in `./src/main/resources/`. 

To override properties at runtime, specify the location of a different properties file. For example:
```
java -jar -Dspring.config.location=/path/to/application.prod.properties ocn-node-1.0.0.jar
``` 

Alternatively, it is possible to provide command line arguments for individual configuration parameters:
```
java -jar -Dserver.port=8081 ocn-node-1.0.0.jar
```

## Properties

Various application properties can be configured, but only a subset of them are featured here. See
Spring Boot [Common Application Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html)
for more.

### `server.port`
Sets the port the OCN Node runs on. [Default: 8080]

### `logging.level.web`
Sets the minimum severity level of logs outputted to stdout. Options: TRACE , DEBUG , INFO , WARN , ERROR , FATAL , OFF.
[Default: INFO] 

### `spring.datasource.url`
Sets the URL of the database the OCN Node should use. By default the OCN Node will use a H2 in-memory database.
The OCN Node also provides a database driver for PostgreSQL. New drivers can be pulled in via the `build.gradle.kts`
configuration file. For example, to add a MYSQL connection, add the following line to the dependencies section:

```
dependencies {
    ...,
    implementation("mysql-connector-java:{{VERSION}}")
}
```

Following an application re-build, it would then be possible to set the datasource URL to a MySQL database URL.

Note that load balanced OCN nodes should all point to the same datasource URL. 

### `spring.database.username`
Sets the database user's username, if using a production database as above.

### `spring.datasource.password`
Sets the password for the database user above.

### `ocn.node.url`
Sets the publicly accessible URL of the OCN Node. It is important that this is set correctly as OCPI platforms
wishing to connect will need to know the correct endpoints of the node's OCPI modules. It should also match
the OCN Registry listing for the node, allowing other nodes on the network to relay messages to it.

Setting this to `http://localhost:8080` for example, will mean that only nodes and OCPI platforms on localhost
can communicate with that node.

When running a node on the test or production network, a reverse proxy might be used such that the URL can
be changed to the domain name of the server, for example `https://ocn.server.net`. Planned OCPI parties would
then be able to start the registration process with the OCN Node at `https://ocn.server.net/ocpi/2.2/versions`.

If load balancing nodes, make sure to set each node's URL to that of the load balancer. 

### `ocn.node.privateKey`
Sets the node's identity on the Open Charging Network. This is used primarily to validate messages sent across the 
network. It should also be set to that which was used to list the node in the OCN Registry.

The OCN uses Ethereum for its public key infrastructure. Such public-private keypairs are commonly managed by
an [Ethereum wallet](https://ethereum.org/wallets/).

As with the node's URL, set the same private key for all nodes under a load balancer if applicable. 

### `ocn.node.apikey`
Sets the node administrator's API Key, commonly used for generating a new TOKEN_A for a planned OCPI platform.
See the [API documentation](https://shareandcharge.bitbucket.io/) for more on the Admin API.

### `ocn.node.dev`
Sets whether the node runs in dev mode. Turning on during local development to allow insecure HTTP connections
(i.e. over localhost). [Default: false]

### `ocn.node.signatures`
Sets whether the node requires signatures in OCPI requests and responses. Note that signatures can also be
enabled/disabled on a party-by-party basis using the *OcnRules* custom OCPI module. It is therefore possible
to override a node's preference to not require message signing (but not the other way around). [Default: true]

For more information about message signing, see the [OCN Notary](https://bitbucket.org/shareandcharge/ocn-notary).

### `ocn.node.stillAliveEnabled`
Sets HubClientInfo module behaviour to run a scheduled task to check the status of connected parties. If enabled,
the node will verify that a registered platform is still connected (i.e. reachable) by sending a GET request to its
OCPI versions endpoint. [Default: true]

In the scenario of load balancing nodes, only one of the nodes should have this behaviour enabled.

### `ocn.node.stillAliveRate`
Sets rate in milliseconds at which the HubClientInfo module runs the still-alive check task, outlined above. 
[Default: 900000 (15 minutes)]

### `ocn.node.plannedPartySearchEnabled`
Sets the HubClientInfo module behaviour to run a scheduled task to check for parties listed in the registry
under the node in question, but have yet to complete the OCPI registration with the node (i.e. are planning 
to connect). [Default: true]

Note: also requires `ocn.node.privateKey` to have been set. 

In the scenario of load balancing nodes, only one of the nodes should have this behaviour enabled.

### `ocn.node.plannedPartySearchRate`
Sets rate in milliseconds at which the HubClientInfo module runs the planned party search task, outlined above. 
[Default: 3600000 (1 hour)]
 
### `ocn.node.web3.provider`
Sets the JSON RPC provider URL for the OCN environment. This is the Ethereum blockchain node which provides
access to the configured Registry smart contract. There are two live networks for the OCN Node: the public test
environment and the production environment. The former runs on the 
[Volta test network](https://energyweb.atlassian.net/wiki/spaces/EWF/pages/702677023/Chain+Volta+Test+Network), whilst
the latter runs on the [Energy Web Chain](https://energyweb.atlassian.net/wiki/spaces/EWF/pages/718078071/Chain+Energy+Web+Chain+Production+Network).

More information about the environments can be found here: 
[test](https://shareandcharge.atlassian.net/wiki/spaces/OCN/pages/409206816/Public+Test+Network) and
[production](https://shareandcharge.atlassian.net/wiki/spaces/OCN/pages/409305103/Production+Network).

### `ocn.node.web3.contracts.registry`
Sets the OCN Registry smart contract address. For the public test environment, this is
`0xd57595D5FA1F94725C426739C449b15D92758D55`. For the production network, this is
 `0x184aeD70F2aaB0Cd1FC62261C1170560cBfd0776`