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

package snc.openchargingnetwork.node.tools

fun String.extractToken() = split(" ").last()

fun String.extractNextLink(): String? {
    val next = split(", ").find { it.contains("; rel=\"next\"") }
    return next?.let {
        val start = it.indexOf('<') + 1
        val end = it.indexOf('>') - 1
        it.slice(IntRange(start, end))
    }
}

fun Map<String, Any?>.filterNull(): Map<String, Any?> {
    return filterValues { it != null }
}
