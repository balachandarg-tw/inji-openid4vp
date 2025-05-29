package io.mosip.sampleapp

import io.mosip.openID4VP.authorizationRequest.VPFormatSupported
import io.mosip.openID4VP.authorizationRequest.Verifier
import io.mosip.openID4VP.authorizationRequest.WalletMetadata
import io.mosip.openID4VP.constants.ClientIdScheme

object HardcodedData {
    val walletMetadata = WalletMetadata(
        vpFormatsSupported = mapOf(
            "ldp_vc" to VPFormatSupported(
                algValuesSupported = listOf(
                    "Ed25519Signature2018",
                    "Ed25519Signature2020",
                    "RSASignature2018"
                )
            ),
            "mso_mdoc" to VPFormatSupported(
                algValuesSupported = listOf("ES256")
            )
        ),
        clientIdSchemesSupported = listOf(
            ClientIdScheme.REDIRECT_URI.value,
            ClientIdScheme.DID.value,
            ClientIdScheme.PRE_REGISTERED.value
        ),
        requestObjectSigningAlgValuesSupported = listOf("EdDSA"),
        authorizationEncryptionAlgValuesSupported = listOf("ECDH-ES"),
        authorizationEncryptionEncValuesSupported = listOf("A256GCM")
    )

    val verifier = Verifier(
        clientId = "https://55d8-2401-4900-7b8a-ade6-149f-f0a8-8bc3-e1d.ngrok-free.app",
        responseUris = listOf("https://55d8-2401-4900-7b8a-ade6-149f-f0a8-8bc3-e1d.ngrok-free.app/redirect")
    )


}
