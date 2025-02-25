package org.fitchfamily.android.dejavu

import java.util.*

/*
*    DejaVu - A location provider backend for microG/UnifiedNlp
*
*    Copyright (C) 2017 Tod Fitch
*    Copyright (C) 2022 Helium314
*
*    This program is Free Software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as
*    published by the Free Software Foundation, either version 3 of the
*    License, or (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

// moved from RfEmitter to separate file

class RfCharacteristics (
    val requiredGpsAccuracy: Float, // Required accuracy for updating emitter coverage (should be less than half of minimumRange, as GPS is frequently off by more then the accuracy)
    val minimumRange: Double,       // Minimum believable coverage radius in meters
    val maximumRange: Double,       // Maximum believable coverage radius in meters
    val minCount: Int               // Minimum number of emitters before we can estimate location
)

enum class EmitterType {
    INVALID,
    WLAN2,
    WLAN5,
    WLAN6,
    BT,
    GSM,
    CDMA,
    WCDMA,
    TDSCDMA,
    LTE,
    NR,
    NR_FR2,
}

private const val METERS: Float = 1.0f
private const val KM = METERS * 1000

val shortRangeEmitterTypes: Set<EmitterType> = EnumSet.of(EmitterType.WLAN5, EmitterType.WLAN6, EmitterType.WLAN2, EmitterType.BT, EmitterType.NR_FR2)

/**
 * Given an emitter type, return the various characteristics we need to know
 * to model it.
 *
 * @return The characteristics needed to model the emitter
 */
fun EmitterType.getRfCharacteristics(): RfCharacteristics =
    when (this) {
        EmitterType.WLAN2 -> characteristicsWlan24
        EmitterType.WLAN5, EmitterType.WLAN6 -> characteristicsWlan5 // small difference in frequency doesn't change range significantly
        EmitterType.GSM -> characteristicsGsm
        // maybe use separate characteristics? but they strongly depend on the used frequency...
        EmitterType.CDMA, EmitterType.WCDMA, EmitterType.TDSCDMA, EmitterType.LTE, EmitterType.NR -> characteristicsLte
        EmitterType.NR_FR2 -> characteristicsNrFr2
        EmitterType.BT -> characteristicsBluetooth
        EmitterType.INVALID -> characteristicsUnknown
    }

// For 2.4 GHz, indoor range seems to be described as about 46 meters
// with outdoor range about 90 meters. Set the minimum range to be about
// 3/4 of the indoor range and the typical range somewhere between
// the indoor and outdoor ranges.
// However we've seem really, really long range detection in rural areas
// so base the move distance on that.
private val characteristicsWlan24 = RfCharacteristics(
    16F * METERS,
    35.0 * METERS,
    300.0 * METERS,  // Seen pretty long detection in very rural areas
    2
)

private val characteristicsWlan5 = RfCharacteristics(
    7F * METERS,
    15.0 * METERS,
    100.0 * METERS,  // Seen pretty long detection in very rural areas
    2
)

// currently not used, planned for stationary beacons if this proves feasible
private val characteristicsBluetooth = RfCharacteristics(
    5F * METERS,
    2.0 * METERS,
    100.0 * METERS, // class 1 devices can have 100 m range
    2
)

private val characteristicsGsm = RfCharacteristics(
    100F * METERS,
    500.0 * METERS,
    200.0 * KM, // usual max is around 35 km, but extended range can be around 200 km
    1
)

// LTE cells are typically much smaller than GSM cells, but could also span the same huge areas.
// "small cells" could actually be some 10 m in size, but assuming all cells might be
// small cells would not be feasible, as it would increase requirements on accuracy and
// lead to bad (overly accurate) location reports for LTE cells only seen once
private val characteristicsLte = RfCharacteristics(
    50F * METERS,
    250.0 * METERS,
    100.0 * KM, // ca 35 km for macrocells, but apparently extended range possible
    1
)

// 5G FR2 supposedly has a range of 300 m, and up to 1 km with beam forming
private val characteristicsNrFr2 = RfCharacteristics(
    25F * METERS,
    70.0 * METERS,
    1000.0 * KM,
    1
)

// Unknown emitter type, just throw out some values that make it unlikely that
// we will ever use it (require too accurate a GPS location, etc.).
private val characteristicsUnknown = RfCharacteristics(
    2F * METERS,
    50.0 * METERS,
    100.0 * METERS,
    99
)
