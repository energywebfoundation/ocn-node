package snc.openchargingnetwork.node.integration

import io.javalin.Javalin

class MspServer(private val port: Int) {

    private val app = Javalin.create().start(port)

    init {

    }

}