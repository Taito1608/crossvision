# OCR-BBox Detection Accuracy Verification Design

## Overview
This document outlines the design for verifying OCR-BBox detection accuracy for 15 images using the PP-OCRv3 ONNX pipeline. The system will:
1. Process all images in OCR/images/ using detection and recognition models (detection is required for localizing text regions)
2. Filter recognized text by allowed characters (from 使用可能文字.md)
3. Handle rotated bounding boxes (already supported by existing detection)
4. Compare results with correct labels from 正解ラベル.csv
5. Calculate exact match rate, partial match rate, and other accuracy metrics
6. Output results to results/batch_full_pipeline2/

## Architecture and Components
- **Input Layer**: Image files from OCR/images/
- **Detection Model**: det.onnx (PP-OCRv3 detection) - REQUIRED for localizing text regions in images
- **Recognition Model**: en_PP-OCRv3_rec.onnx (English recognition)
- **Character Dictionary**: en_PP-OCRv3_dict.txt
- **Allowed Characters Filter**: Loads from 使用可能文字.md
- **Label Comparison**: Reads 正解ラベル.csv
- **Accuracy Calculator**: Computes exact/partial match rates
- **Output Layer**: Results saved to results/batch_full_pipeline2/

## Data Flow
1. Load allowed character set from 使用可能文字.md
2. Load correct labels from 正解ラベル.csv
3. For each image in OCR/images/:
   a. Run detection model to get bounding boxes and angles
   b. For each detection:
      i. Extract and preprocess crop
      ii. Run recognition model
      iii. Apply CTC decoding
      iv. Filter recognized text by allowed character set
      v. Extract product codes using regex pattern
   c. Compare extracted product codes with correct labels
   d. Calculate exact match (all codes match) and partial match (any code matches)
   e. Save visualization image and JSON data
4. Generate summary report with accuracy metrics

## Error Handling and Testing
- **Error Handling**:
  - Failed image loading: Skip and log error
  - Model inference failures: Catch exceptions and continue
  - Empty detection results: Handle gracefully
  - File I/O errors: Use try-catch blocks
- **Testing Approach**:
  - Verify against known correct labels
  - Check that filtered text contains only allowed characters
  - Validate that output directory structure is correct
  - Ensure summary report includes all processed images

## Design Approaches Considered
1. **Modify existing batch_full_pipeline.py**: Add filtering and accuracy calculation directly
   - Pros: Simple, single script
   - Couples verification with core pipeline logic
2. **Create new verification script building on batch_full_pipeline.py** (Selected):
   - Pros: Separation of concerns, reusable core pipeline
   - Cons: Slightly more complex initially
3. **Post-processing script reading existing batch results**:
   - Pros: Decoupled from processing
   - Cons: Requires running pipeline twice, less efficient

Selected approach 2 for best balance of maintainability and efficiency.