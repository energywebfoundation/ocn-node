package snc.openchargingnetwork.client.models.ocpi

import com.fasterxml.jackson.annotation.JsonProperty

data class Versions(@JsonProperty("versions") val versions: List<Version>)

data class Version(@JsonProperty("version") val version: String,
                   @JsonProperty("url") val url: String)

data class VersionDetail(@JsonProperty("version") val version: String,
                         @JsonProperty("endpoints") val endpoints: List<Endpoint>)

data class Endpoint(@JsonProperty("identifier") val identifier: String,
                    @JsonProperty("role") val role: InterfaceRole,
                    @JsonProperty("url") val url: String)