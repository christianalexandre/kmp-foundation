package com.kmpfoundation.core.platform

import android.os.Build

actual fun platformName(): String = "Android ${Build.VERSION.RELEASE}"
