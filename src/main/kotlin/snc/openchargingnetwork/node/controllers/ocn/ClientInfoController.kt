package snc.openchargingnetwork.node.controllers.ocn

import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.services.HubClientInfoService
import snc.openchargingnetwork.node.services.WalletService

@RestController
@RequestMapping("\${ocn.node.apiPrefix}/ocn/message/ocn/client-info")
class ClientInfoController(private val hubClientInfoService: HubClientInfoService,
                           private val walletService: WalletService) {

    @PutMapping
    fun updateClientInfo(@RequestHeader("OCN-Signature") signature: String,
                         @RequestBody body: String) {

        val clientInfo = walletService.verifyClientInfo(body, signature)

        // save all received client info (even if connected parties are not interested, they might be in the future)
        hubClientInfoService.saveClientInfo(clientInfo)

        val parties = hubClientInfoService.getPartiesToNotifyOfClientInfoChange(clientInfo = clientInfo)
        if (parties.isNotEmpty()) {
            hubClientInfoService.notifyPartiesOfClientInfoChange(parties, clientInfo)
        }
    }

}
