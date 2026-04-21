cmake_minimum_required(VERSION 3.18.1)

project(onnx_ocr_engine)

# ONNX Runtimeのパスを設定（後でaarから展開したライブラリを使用）
set(ONNX_RUNTIME_ROOT /home/gohan/onnxruntime-android)

# include path
include_directories(
    ${ONNX_RUNTIME_ROOT}/include
)

# native library
add_library(
    onnx_ocr_engine
    SHARED
    src/main/cpp/onnx_ocr_engine.cpp
)

# link ONNX Runtime library
find_library(
    onnxruntime-lib
    onnxruntime
    ${ONNX_RUNTIME_ROOT}/lib
)

target_link_libraries(
    onnx_ocr_engine
    ${onnxruntime-lib}
    log
    android
)
