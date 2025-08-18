package snc.openchargingnetwork.node.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Address
import snc.openchargingnetwork.node.models.ocpi.Role

enum class PaymentStatus {
    NOT_PAID,
    PAID,
    PENDING
}

enum class CvStatus {
    NOT_VERIFIED,
    VERIFIED,
    PENDING
}

@Serializable
data class Party(
    var id: String,
    var countryCode: String,
    var partyId: String,
    var partyAddress: String,
    var roles: List<Role> = emptyList<Role>(),
    var name: String,
    var url: String,
    var paymentStatus: PaymentStatus,
    var cvStatus: CvStatus,
    var active: Boolean = false,
    var deleted: Boolean = false,
    var operator: Operator
)

@Serializable
data class Operator(
    var id: String,
    var domain: String,
    var parties: List<Party> = emptyList()
)

@Serializable
data class OcnRegistry(
    var parties: List<Party> = emptyList(),
    var operators: List<Operator> = emptyList()
)

@Serializable
data class GqlQuery(val query: String, val operationName: String, val variables: Map<String, String>)

@Serializable
data class GqlPartiesAndOpsData(
    val parties: List<Party>? = null,
    val party: Party? = null,
    val operators: List<Operator>? = null,
    val operator: Operator? = null,
)

@Serializable
data class EmpCertificate (
    val identifier: String,
    val name: String,
    val marktfunktion: String,
    val bilanzkreis: String,
    val lieferant: String,
    val vatid: String,
    val billingAddress: String,
    val billingCity: String,
    val billingPostalCode: String,
    val billingCountry: String,
    val billingEmail: String,
    val owner: String,
    val blockNumber: Int
)

@Serializable
data class CpoCertificate (
    val identifier: String,
    val name: String,
    val owner: String,
    val blockNumber: Int
)

@Serializable
data class OtherCertificate (
    val identifier: String,
    val name: String,
    val owner: String,
    val blockNumber: Int
)

@Serializable
data class GqlCertificateData(
    @SerialName("empverifieds") val emp: List<EmpCertificate>,
    @SerialName("cpoverifieds") val cpo: List<CpoCertificate>,
    @SerialName("otherVerifieds") val other: List<OtherCertificate>,
)

@Serializable
data class GqlCertificateDataResponse(
    @SerialName("EMP") val emp: List<EmpCertificate>,
    @SerialName("CPO") val cpo: List<CpoCertificate>,
    @SerialName("OTHER") val other: List<OtherCertificate>,
)

@Serializable
data class GqlError(val message: String? = null)

@Serializable
data class GqlResponse<T>(val data: T? = null, val errors: List<GqlError>? = null)