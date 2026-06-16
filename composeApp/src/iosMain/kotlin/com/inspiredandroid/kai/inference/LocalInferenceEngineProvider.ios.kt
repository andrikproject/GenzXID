package com.genzxid.app.inference

actual fun createLocalInferenceEngine(): LocalInferenceEngine? = IosLiteRTInferenceEngine()
