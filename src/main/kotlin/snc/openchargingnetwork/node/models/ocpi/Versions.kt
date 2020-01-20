/*
    Copyright 2019 Share&Charge Foundation

    This file is part of Open Charging Network Node.

    Open Charging Network Node is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Node is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Node.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.node.models.ocpi

import com.fasterxml.jackson.annotation.JsonProperty

data class Version(@JsonProperty("version") val version: String,
                   @JsonProperty("url") val url: String)

data class VersionDetail(@JsonProperty("version") val version: String,
                         @JsonProperty("endpoints") val endpoints: List<Endpoint>)

data class Endpoint(@JsonProperty("identifier") val identifier: String,
                    @JsonProperty("role") val role: InterfaceRole,
                    @JsonProperty("url") val url: String)