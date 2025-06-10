package io.mosip.sampleapp;

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.*
import java.security.*
import java.security.interfaces.*
import java.util.*

// Enum for key types
enum class KeyType {
    RSA, ES256
}

// Wrapper for result
data class SignedVPJWT(
    val jwt: String,
    val publicJWK: String,
    val algorithm: String
)

object VPTokenSigner {

    fun generateKeyPair(keyType: KeyType): KeyPair {
        return when (keyType) {
            KeyType.RSA -> {
                val keyGen = KeyPairGenerator.getInstance("RSA")
                keyGen.initialize(2048)
                keyGen.generateKeyPair()
            }
            KeyType.ES256 -> {
                val keyGen = KeyPairGenerator.getInstance("EC")
                keyGen.initialize(Curve.P_256.toECParameterSpec())
                keyGen.generateKeyPair()
            }
        }
    }

    fun signVPToken(
        keyPair: KeyPair,
        keyType: KeyType,
        vpPayload: Map<String, Any>
    ): SignedVPJWT {
        val jwk: JWK
        val signer: JWSSigner
        val alg: JWSAlgorithm

        when (keyType) {
            KeyType.RSA -> {
                val rsaKey = RSAKey.Builder(keyPair.public as RSAPublicKey)
                    .privateKey(keyPair.private as RSAPrivateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build()
                jwk = rsaKey
                signer = RSASSASigner(rsaKey)
                alg = JWSAlgorithm.RS256
            }
            KeyType.ES256 -> {
                val ecKey = ECKey.Builder(Curve.P_256, keyPair.public as ECPublicKey)
                    .privateKey(keyPair.private as ECPrivateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build()
                jwk = ecKey
                signer = ECDSASigner(ecKey)
                alg = JWSAlgorithm.ES256
            }
        }

        val claimsSet = JWTClaimsSet.Builder()
            .issuer("did:example:holder")
            .issueTime(Date())
            .claim("vp", vpPayload)
            .build()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(alg).keyID(jwk.keyID).type(JOSEObjectType.JWT).build(),
            claimsSet
        )
        signedJWT.sign(signer)

        return SignedVPJWT(
            jwt = signedJWT.serialize(),
            publicJWK = jwk.toPublicJWK().toJSONString(),
            algorithm = alg.name
        )
    }

    fun signDeviceAuthentication(
        keyPair: KeyPair,
        keyType: KeyType,
        deviceAuthBytes: ByteArray
    ): SignedVPJWT {
        val header = JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build()
        val payload = Payload(deviceAuthBytes)
        val jwsObject = JWSObject(header, payload)

        val signer = when (keyType) {
            KeyType.ES256 -> ECDSASigner(keyPair.private as ECPrivateKey)
            KeyType.RSA -> RSASSASigner(keyPair.private)
            // Add other key types if needed
        }
        jwsObject.sign(signer)

        return SignedVPJWT(
            jwt = jwsObject.serialize(),
            algorithm = jwsObject.header.algorithm.name,
            publicJWK = "" // Optionally, serialize public key as JWK if needed
        )
    }
}
