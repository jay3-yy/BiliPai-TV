package com.android.purebilibili.feature.cast

import android.os.Parcel
import android.os.Parcelable

/**
 * Cross-process safe cast device descriptor.
 */
data class CastDeviceInfo(
    val udn: String,
    val name: String,
    val description: String,
    val location: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        udn = parcel.readString().orEmpty(),
        name = parcel.readString().orEmpty(),
        description = parcel.readString().orEmpty(),
        location = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(udn)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(location)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CastDeviceInfo> {
        override fun createFromParcel(parcel: Parcel): CastDeviceInfo = CastDeviceInfo(parcel)

        override fun newArray(size: Int): Array<CastDeviceInfo?> = arrayOfNulls(size)
    }
}
