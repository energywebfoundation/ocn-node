package snc.openchargingnetwork.client.models.ocpi

import com.fasterxml.jackson.annotation.JsonProperty

data class Versions(@JsonProperty("versions") val versions: Array<Version>) {

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

data class Version(@JsonProperty("version") val version: String,
                   @JsonProperty("url") val url: String)

data class VersionDetail(@JsonProperty("version") val version: String,
                         @JsonProperty("endpoints") val endpoints: Array<Endpoint>) {

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

data class Endpoint(@JsonProperty("identifier") val identifier: String,
                    @JsonProperty("role") val role: InterfaceRole,
                    @JsonProperty("url") val url: String)