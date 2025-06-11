package io.mosip.sampleapp.utils

import io.mosip.openID4VP.constants.FormatType
import io.mosip.sampleapp.VCWithFormat

object Utils {
    fun getDisplayLabel(vcWithFormat: VCWithFormat): String? {
        val typeLabel = when (vcWithFormat.format) {
            FormatType.LDP_VC.value -> {
                val typeArray = vcWithFormat.vc.getAsJsonArray("type")
                if (typeArray != null && typeArray.size() > 1) {
                    typeArray[1].asString
                } else {
                    "-"
                }
            }

            FormatType.MSO_MDOC.value -> {
                "MDL Driving License"
            }

            else -> "-"
        }
        return typeLabel
    }
}