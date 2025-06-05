package io.mosip.sampleapp.vc

import com.google.gson.Gson
import com.google.gson.JsonObject

object HardcodedVC {
    const val MOSIP_VC = """
{
    "@context": [
        "https://www.w3.org/2018/credentials/v1/mosip",
        "https://schema.org/"
    ],
    "credentialSubject": {
        "gender": [
            {
                "language": "eng",
                "value": "Male"
            },
            {
                "language": "fra",
                "value": "Mâle"
            },
            {
                "language": "ara",
                "value": "ذكر"
            }
        ],
        "postalCode": "45009",
        "fullName": [
            {
                "language": "fra",
                "value": "Siddharth K Mansour"
            },
            {
                "language": "ara",
                "value": "تتگلدكنسَزقهِقِفل دسييسيكدكنوڤو"
            },
            {
                "language": "eng",
                "value": "Siddharth K Mansour"
            }
        ],
        "dateOfBirth": "1987/11/25",
        "face": "sqauare logo",
        "province": [
            {
                "language": "fra",
                "value": "yuān 2"
            },
            {
                "language": "ara",
                "value": "يَُانꉛ⥍"
            },
            {
                "language": "eng",
                "value": "yuan wee"
            }
        ],
        "phone": "+919427357934",
        "addressLine1": [
            {
                "language": "fra",
                "value": "yuān⥍"
            },
            {
                "language": "ara",
                "value": ""
            },
            {
                "language": "eng",
                "value": "Slung"
            }
        ],
        "vcVer": "VC-V1",
        "id": "https://api.dev1.mosip.net/v1/mock-identity-system/identity/1234567",
        "UIN": "1234567",
        "region": [
            {
                "language": "fra",
                "value": "yuān 3"
            },
            {
                "language": "ara",
                "value": ""
            },
            {
                "language": "eng",
                "value": "yuan wee 3"
            }
        ],
        "email": "siddhartha.km@gmail.com"
    },
    "id": "did:uuid:1d93e315-d979-480b-a7a2-a0ff01f1856f",
    "issuanceDate": "2024-11-06T10:44:19.044Z",
    "issuer": "did:example:123456789",
    "proof": {
        "created": "2024-11-06T10:44:19Z",
        "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJraWQiOiJLYlJXRU9YQ0pVRENWVnVET2ZsSkRQWnAtXzNqMEZvajd1RVZHd19xOEdzIiwiYWxnIjoiUFMyNTYifQ..NEcXf5IuDf0eJcBbtIBsXC2bZeOzNBduWG7Vz9A3ePcvh-SuwggPcCPQLrdgl79ta5bYsKsJSKVSS0Xg-GvlY71I2OzU778Bkq52LIDtSXY3DrxQEvM-BqjKLBB-ScA850pG2gV-k_8nkCPmAdvda_jj2Vlkss7VPB5LI6skWTgM4MOyvlMzZCzqmifqTzHLVgefzfixld7E38X7wxzEZfn2lY_fRfWqcL8pKL_kijTHwdTWLb9hMQtP9vlk2iarbT8TmZqutZD8etd1PBFm7V_izcY9cO75A4N3fVrr6NC50cDHDshPZFS48uTBDK-SSePxibpmq1afaS_VX6kX7A",
        "proofPurpose": "assertionMethod",
        "type": "RsaSignature2018",
        "verificationMethod": "https://api.dev1.mosip.net/.well-known/ida-public-key.json"
    },
    "type": [
        "VerifiableCredential",
        "MosipVerifiableCredential"
    ]
}
    """

    const val INSURANCE_VC = """
{
    "@context": [
        "https://www.w3.org/2018/credentials/v1/insurance",
        "https://schema.org/"
    ],
    "credentialSubject": {
        "gender": [
            {
                "language": "eng",
                "value": "Male"
            },
            {
                "language": "fra",
                "value": "Mâle"
            },
            {
                "language": "ara",
                "value": "ذكر"
            }
        ],
        "postalCode": "45009",
        "fullName": [
            {
                "language": "fra",
                "value": "Siddharth K Mansour"
            },
            {
                "language": "ara",
                "value": "تتگلدكنسَزقهِقِفل دسييسيكدكنوڤو"
            },
            {
                "language": "eng",
                "value": "Siddharth K Mansour"
            }
        ],
        "dateOfBirth": "1987/11/25",
        "face": "sqauare logo",
        "province": [
            {
                "language": "fra",
                "value": "yuān 2"
            },
            {
                "language": "ara",
                "value": "يَُانꉛ⥍"
            },
            {
                "language": "eng",
                "value": "yuan wee"
            }
        ],
        "phone": "+919427357934",
        "addressLine1": [
            {
                "language": "fra",
                "value": "yuān⥍"
            },
            {
                "language": "ara",
                "value": ""
            },
            {
                "language": "eng",
                "value": "Slung"
            }
        ],
        "vcVer": "VC-V1",
        "id": "https://api.dev1.mosip.net/v1/mock-identity-system/identity/1234567",
        "UIN": "1234567",
        "region": [
            {
                "language": "fra",
                "value": "yuān 3"
            },
            {
                "language": "ara",
                "value": ""
            },
            {
                "language": "eng",
                "value": "yuan wee 3"
            }
        ],
        "email": "siddhartha.km@gmail.com"
    },
    "id": "did:uuid:1d93e315-d979-480b-a7a2-a0ff01f1856f",
    "issuanceDate": "2024-11-06T10:44:19.044Z",
    "issuer": "did:example:123456789",
    "proof": {
        "created": "2024-11-06T10:44:19Z",
        "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJraWQiOiJLYlJXRU9YQ0pVRENWVnVET2ZsSkRQWnAtXzNqMEZvajd1RVZHd19xOEdzIiwiYWxnIjoiUFMyNTYifQ..NEcXf5IuDf0eJcBbtIBsXC2bZeOzNBduWG7Vz9A3ePcvh-SuwggPcCPQLrdgl79ta5bYsKsJSKVSS0Xg-GvlY71I2OzU778Bkq52LIDtSXY3DrxQEvM-BqjKLBB-ScA850pG2gV-k_8nkCPmAdvda_jj2Vlkss7VPB5LI6skWTgM4MOyvlMzZCzqmifqTzHLVgefzfixld7E38X7wxzEZfn2lY_fRfWqcL8pKL_kijTHwdTWLb9hMQtP9vlk2iarbT8TmZqutZD8etd1PBFm7V_izcY9cO75A4N3fVrr6NC50cDHDshPZFS48uTBDK-SSePxibpmq1afaS_VX6kX7A",
        "proofPurpose": "assertionMethod",
        "type": "RsaSignature2018",
        "verificationMethod": "https://api.dev1.mosip.net/.well-known/ida-public-key.json"
    },
    "type": [
        "VerifiableCredential",
        "InsuranceVerifiableCredential"
    ]
}
    """

    const val MOCK_VC = """
{
    "@context": [
        "https://www.w3.org/2018/credentials/v1/mock",
        "https://schema.org/"
    ],
    "credentialSubject": {
        "gender": [
            {
                "language": "eng",
                "value": "Male"
            },
            {
                "language": "fra",
                "value": "Mâle"
            },
            {
                "language": "ara",
                "value": "ذكر"
            }
        ],
        "postalCode": "45009",
        "fullName": [
            {
                "language": "fra",
                "value": "Siddharth K Mansour"
            },
            {
                "language": "ara",
                "value": "تتگلدكنسَزقهِقِفل دسييسيكدكنوڤو"
            },
            {
                "language": "eng",
                "value": "Siddharth K Mansour"
            }
        ],
        "dateOfBirth": "1987/11/25",
        "face": "sqauare logo",
        "province": [
            {
                "language": "fra",
                "value": "yuān 2"
            },
            {
                "language": "ara",
                "value": "يَُانꉛ⥍"
            },
            {
                "language": "eng",
                "value": "yuan wee"
            }
        ],
        "phone": "+919427357934",
        "addressLine1": [
            {
                "language": "fra",
                "value": "yuān⥍"
            },
            {
                "language": "ara",
                "value": ""
            },
            {
                "language": "eng",
                "value": "Slung"
            }
        ],
        "vcVer": "VC-V1",
        "id": "https://api.dev1.mosip.net/v1/mock-identity-system/identity/1234567",
        "UIN": "1234567",
        "region": [
            {
                "language": "fra",
                "value": "yuān 3"
            },
            {
                "language": "ara",
                "value": ""
            },
            {
                "language": "eng",
                "value": "yuan wee 3"
            }
        ],
        "email": "siddhartha.km@gmail.com"
    },
    "id": "did:uuid:1d93e315-d979-480b-a7a2-a0ff01f1856f",
    "issuanceDate": "2024-11-06T10:44:19.044Z",
    "issuer": "did:example:123456789",
    "proof": {
        "created": "2024-11-06T10:44:19Z",
        "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJraWQiOiJLYlJXRU9YQ0pVRENWVnVET2ZsSkRQWnAtXzNqMEZvajd1RVZHd19xOEdzIiwiYWxnIjoiUFMyNTYifQ..NEcXf5IuDf0eJcBbtIBsXC2bZeOzNBduWG7Vz9A3ePcvh-SuwggPcCPQLrdgl79ta5bYsKsJSKVSS0Xg-GvlY71I2OzU778Bkq52LIDtSXY3DrxQEvM-BqjKLBB-ScA850pG2gV-k_8nkCPmAdvda_jj2Vlkss7VPB5LI6skWTgM4MOyvlMzZCzqmifqTzHLVgefzfixld7E38X7wxzEZfn2lY_fRfWqcL8pKL_kijTHwdTWLb9hMQtP9vlk2iarbT8TmZqutZD8etd1PBFm7V_izcY9cO75A4N3fVrr6NC50cDHDshPZFS48uTBDK-SSePxibpmq1afaS_VX6kX7A",
        "proofPurpose": "assertionMethod",
        "type": "RsaSignature2018",
        "verificationMethod": "https://api.dev1.mosip.net/.well-known/ida-public-key.json"
    },
    "type": [
        "VerifiableCredential",
        "MockVerifiableCredential"
    ]
}
    """

    fun get(index: Int): VCWithFormat {
        val gson = Gson()
        return when (index) {
            0 -> VCWithFormat("ldp_vc", gson.fromJson(MOSIP_VC, JsonObject::class.java))
            1 -> VCWithFormat("ldp_vc", gson.fromJson(INSURANCE_VC, JsonObject::class.java))
            else -> VCWithFormat("ldp_vc", gson.fromJson(MOCK_VC, JsonObject::class.java))
        }
    }
}

data class VCWithFormat(
    val format: String,
    val vc: JsonObject
)
