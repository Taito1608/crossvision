#include <jni.h>
#include <string>
#include <onnxruntime/onnxruntime_c_api.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_solution__development_ocr_OnnxOcrEngine_initModel(
        JNIEnv* env,
        jobject /* this */) {
    OrtStatus* status = Ort::CreateEnv(OrtLoggingLevel::ORT_LOGGING_LEVEL_WARNING, "ONNX");
    if (status) {
        const char* msg = OrtGetErrorMessage(status);
        return env->NewStringUTF(msg);
    }
    return env->NewStringUTF("ONNX Runtime initialized");
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_solution__development_ocr_OnnxOcrEngine_runInference(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray input) {
    jfloat* inputPtr = env->GetFloatArrayElements(input, nullptr);
    jsize inputSize = env->GetArrayLength(input);
    
    // Create output array (dummy: 1000 floats)
    jfloatArray output = env->NewFloatArray(1000);
    jfloat outputData[1000];
    for (int i = 0; i < 1000; i++) {
        outputData[i] = 0.0f;
    }
    env->SetFloatArrayRegion(output, 0, 1000, outputData);
    
    env->ReleaseFloatArrayElements(input, inputPtr, JNI_ABORT);
    return output;
}
