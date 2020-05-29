package snc.openchargingnetwork.node.models.events

import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables


class AppRecipientFoundEvent(val recipient: BasicRole, val request: OcpiRequestVariables)
