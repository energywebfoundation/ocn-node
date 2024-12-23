/*
    Copyright 2019-2020 eMobilify GmbH

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package snc.openchargingnetwork.node.controllers.ocn

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder
import snc.openchargingnetwork.node.models.ocpi.OcpiResponse


@RestController
@RequestMapping("\${ocn.node.apiPrefix}/ocn/message")
class MessageController(private val requestHandlerBuilder: OcpiRequestHandlerBuilder) {

    @PostMapping
    fun postMessage(@RequestHeader("X-Request-ID") requestID: String,
                    @RequestHeader("OCN-Signature") signature: String,
                    @RequestBody body: String): ResponseEntity<OcpiResponse<Any>> {

        return requestHandlerBuilder
                .build<Any>(body)
                .forwardFromOcn(signature)
                .getResponseWithAllHeaders()
    }

}