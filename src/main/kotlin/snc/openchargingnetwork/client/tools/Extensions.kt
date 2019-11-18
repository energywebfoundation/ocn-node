/*
    Copyright 2019 Share&Charge Foundation

    This file is part of Open Charging Network Client.

    Open Charging Network Client is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Client is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Client.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.client.tools

fun String.extractToken() = split(" ").last()

fun String.extractNextLink(): String? {
    val next = split(", ").find { it.contains("; rel=\"next\"") }
    return next?.let {
        val start = it.indexOf('<') + 1
        val end = it.indexOf('>') - 1
        it.slice(IntRange(start, end))
    }
}
