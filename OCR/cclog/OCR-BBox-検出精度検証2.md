## User (checkpoint)

OCR-BBox-検出精度検証2

[@OCR-BBox-検出精度検証.md](file:///mnt/c/Users/soyak/StudioProjects/crossvision/OCR/cclog/OCR-BBox-%E6%A4%9C%E5%87%BA%E7%B2%BE%E5%BA%A6%E6%A4%9C%E8%A8%BC.md) 
コチラの続きをしてください。
最後Errorで止まっちゃったので、再開して実行して検証できるようにしてください

## Assistant



Let me first read the design document and check the current state of the relevant files.

**Tool Call: Read OCR/cclog/OCR-BBox-検出精度検証.md**
Status: Failed

```
File content (29598 tokens) exceeds maximum allowed tokens (25000). Use offset and limit parameters to read specific portions of the file, or search for specific content instead of reading the whole file.
```

## Assistant




**Tool Call: ls -la /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/**
Status: Completed

Terminal:
```
total 3772
drwxr-xr-x 1 gohan gohan    4096 May  9 16:08 .
drwxr-xr-x 1 gohan gohan    4096 May  9 14:17 ..
-rw-r--r-- 1 gohan gohan    3577 May  9 14:21 PaddleOCR2-by-nogami.py
-rw-r--r-- 1 gohan gohan    9546 May  9 14:09 batch_full_pipeline.py
-rw-r--r-- 1 gohan gohan 3813189 May  9 16:08 figurecolor_test.ipynb
-rw-r--r-- 1 gohan gohan   14671 May  9 12:24 ocr_eval.py
-rw-r--r-- 1 gohan gohan    4620 May  9 13:06 verify_detection_angle.py
-rw-r--r-- 1 gohan gohan    7099 May  9 13:11 verify_full_pipeline.py
```


## Assistant




**Tool Call: ls -la /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/results/**
Status: Completed

Terminal:
```
total 2108
drwxr-xr-x 1 gohan gohan    4096 May  9 14:11 .
drwxr-xr-x 1 gohan gohan    4096 May  9 14:17 ..
-rw-r--r-- 1 gohan gohan 1063825 May  9 13:06 M4sb29-2_detection_angle.jpg
-rw-r--r-- 1 gohan gohan     650 May  9 13:06 M4sb29-2_detection_angle.json
-rw-r--r-- 1 gohan gohan 1071063 May  9 13:11 M4sb29-2_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan     967 May  9 13:11 M4sb29-2_full_pipeline.json
drwxr-xr-x 1 gohan gohan    4096 May  9 14:11 batch_full_pipeline
-rw-r--r-- 1 gohan gohan   10174 May  9 12:25 eval_results.json
```


**Tool Call: Read OCR/cclog/OCR-BBox-検出精度検証.md (1 - 100)**
Status: Completed

```
1	## User
2	
3	OCR-BBox-検出精度検証
4	
5	[@OCR-BBox-14枚に対しての検出.md](file:///mnt/c/Users/soyak/StudioProjects/crossvision/OCR/cclog/OCR-BBox-14%E6%9E%9A%E3%81%AB%E5%AF%BE%E3%81%97%E3%81%A6%E3%81%AE%E6%A4%9C%E5%87%BA.md) 
6	コチラのチャットの続きです。
7	Errorが出て止まってしまったので再度最後に送ったプロンプトを以下に貼っておきます。
8	
9	んっと、制約があるので、どこかに明記しておいて欲しいです。
10	[@使用可能文字.md](file:///mnt/c/Users/soyak/StudioProjects/crossvision/OCR/%E4%BD%BF%E7%94%A8%E5%8F%AF%E8%83%BD%E6%96%87%E5%AD%97.md) 
11	ここに使用可能文字を書いておきました。
12	確か64文字？68文字？くらいが使用可能文字です。
13	まとめ直してください。
14	
15	とまぁこの用に使用可能文字が決まっているので、日本語ラベルや特殊フォントとかは本当に関係ないです。
16	また、[@PaddleOCR2-by-nogami.py](file:///mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/PaddleOCR2-by-nogami.py) コチラは友人が作成してくれたスクリプトです。
17	コチラも上手く組み込めないかな？
18	あと、文字列が斜めにある場合に、バウンディングボックスも斜めに出来ないかな？
19	それから、[@正解ラベル.csv](file:///mnt/c/Users/soyak/StudioProjects/crossvision/OCR/images/%E6%AD%A3%E8%A7%A3%E3%83%A9%E3%83%99%E3%83%AB.csv) ここに正解ラベルもあるので、それと照合して一致率、完全一致と部分一致も計算するようにしてください。
20	もう一度出力results/batch_full_pipeline2とかで評価してください
21	
22	## Assistant
23	
24	<thinking>
25	We are at the beginning of the conversation. The user has provided a context about an OCR project and wants to continue with the verification of detection accuracy for bounding boxes.
26	
27	 The user's message includes:
28	 - A reference to a markdown file about OCR-BBox detection for 14 images (actually 15) that had an error and stopped.
29	 - They have written the allowed characters in a file: [使用可能文字.md] (which is in the OCR directory).
30	 - They mention a friend's script: [PaddleOCR2-by-nogami.py] in the scripts directory.
31	 - They want to incorporate that script.
32	 - They want the bounding box to be able to be drawn at an angle (rotated) when the text is slanted.
33	 - They have a correct label CSV: [正解ラベル.csv] in the OCR/images directory.
34	 - They want to calculate match rates (exact and partial) by comparing with the correct labels.
35	 - They want to output the results to `results/batch_full_pipeline2` and evaluate.
36	
37	 Steps to take:
38	
39	 1. First, we must invoke the `using-superpowers` skill as per the system reminder. This is the first step in any conversation.
40	
41	 2. Then, we should look at the current state of the OCR project, especially the files mentioned.
42	
43	 3. We need to check the existing code and scripts to understand the current pipeline.
44	
45	 4. We are to create a verification pipeline that:
46	    - Uses the allowed characters set.
47	    - Incorporates the friend's script (PaddleOCR2-by-nogami.py) if possible.
48	    - Handles rotated bounding boxes (if the text is slanted).
49	    - Compares the OCR results with the correct labels (from the CSV) and calculates exact and partial match rates.
50	    - Outputs the results to `results/batch_full_pipeline2`.
51	
52	 However, note that the user has already done some work and there are existing files in the OCR directory.
53	
54	 Let's start by invoking the required skill and then exploring the project.
55	
56	 Important: We must use the `Skill` tool to invoke `using-superpowers` before any response.
57	
58	 Since the system reminder says: "Use when starting any conversation - establishes how to find and use skills, requiring Skill tool invocation before ANY response including clarifying questions"
59	
60	 We will do:
61	
62	   Skill: using-superpowers
63	
64	 Then, we can proceed to explore the project.
65	
66	 But note: the user has provided a lot of context in the message. We are to continue from where the previous chat left off.
67	
68	 Let's first check the current directory and then look at the OCR directory structure.
69	
70	 However, note that the system also reminds us about the MCP tools: code-review-graph. We must use the graph tools first for exploring the codebase.
71	
72	 But since we are just starting and the user has not asked a specific code exploration question, we are to set up the skill and then maybe do a brief exploration.
73	
74	 Let's break down:
75	
76	 Step 1: Invoke the using-superpowers skill.
77	
78	 Step 2: Then, we can use the MCP tools to get an overview of the project or look at specific files.
79	
80	 However, the user's request is about continuing the OCR-BBox detection accuracy verification.
81	
82	 We are to:
83	
84	   - Check the allowed characters file.
85	   - Look at the friend's script.
86	   - Check the correct label CSV.
87	   - Then, design a verification script that uses the allowed characters, handles rotated bounding boxes, and compares with the labels.
88	
89	 Given the complexity, we might want to create a plan.
90	
91	 But note: the system reminder also says that we must use the `superpowers:brainstorming` skill before any creative work (creating features, building components, etc.). However, the user is asking for a specific continuation.
92	
93	 Since the user is asking for a specific task (to set up the verification pipeline with specific requirements), we might not need brainstorming if we are just implementing a clear set of steps.
94	
95	 However, to be safe, let's follow the flow:
96	
97	   - We are starting the conversation -> use `using-superpowers`.
98	   - Then, we are about to do creative work (we are going to create or modify scripts for verification) -> so we should use `brainstorming` skill.
99	
100	 But note: the user has already described what they want. We are to implement it.
```

## Assistant




**Tool Call: ls -la /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/results/batch_full_pipeline/**
Status: Completed

Terminal:
```
total 50616
drwxr-xr-x 1 gohan gohan    4096 May  9 14:11 .
drwxr-xr-x 1 gohan gohan    4096 May  9 14:11 ..
-rw-r--r-- 1 gohan gohan 7000859 May  9 14:11 015_B15b30N-41__B15b30N-7A_45deg_NW_yellow_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    4254 May  9 14:11 015_B15b30N-41__B15b30N-7A_45deg_NW_yellow_full_pipeline.json
-rw-r--r-- 1 gohan gohan 7297467 May  9 14:11 IMG20250416153020_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    3427 May  9 14:11 IMG20250416153020_full_pipeline.json
-rw-r--r-- 1 gohan gohan 2189281 May  9 14:11 IMG20250416160821_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    3719 May  9 14:11 IMG20250416160821_full_pipeline.json
-rw-r--r-- 1 gohan gohan 2068891 May  9 14:11 IMG_0422_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    7600 May  9 14:11 IMG_0422_full_pipeline.json
-rw-r--r-- 1 gohan gohan 2592355 May  9 14:11 IMG_0771_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    2484 May  9 14:11 IMG_0771_full_pipeline.json
-rw-r--r-- 1 gohan gohan 2176370 May  9 14:11 IMG_0787_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    1083 May  9 14:11 IMG_0787_full_pipeline.json
-rw-r--r-- 1 gohan gohan 2597375 May  9 14:11 IMG_1536_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan     573 May  9 14:11 IMG_1536_full_pipeline.json
-rw-r--r-- 1 gohan gohan 3134708 May  9 14:11 IMG_1537_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    1017 May  9 14:11 IMG_1537_full_pipeline.json
-rw-r--r-- 1 gohan gohan 2604138 May  9 14:11 IMG_1548_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    1009 May  9 14:11 IMG_1548_full_pipeline.json
-rw-r--r-- 1 gohan gohan 2784246 May  9 14:11 IMG_1571_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    1916 May  9 14:11 IMG_1571_full_pipeline.json
-rw-r--r-- 1 gohan gohan 1068367 May  9 14:11 M4sb29-2_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    1032 May  9 14:11 M4sb29-2_full_pipeline.json
-rw-r--r-- 1 gohan gohan 1155568 May  9 14:11 M5sb24-10_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    1510 May  9 14:11 M5sb24-10_full_pipeline.json
-rw-r--r-- 1 gohan gohan 1577124 May  9 14:11 N5G15-X1Y1_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan    1080 May  9 14:11 N5G15-X1Y1_full_pipeline.json
-rw-r--r-- 1 gohan gohan    3139 May  9 14:11 _summary.json
-rw-r--r-- 1 gohan gohan 5038807 May  9 14:11 multi_001_sunny_yellow_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan   10526 May  9 14:11 multi_001_sunny_yellow_full_pipeline.json
-rw-r--r-- 1 gohan gohan 8427493 May  9 14:11 multi_008_yellow_green_full_pipeline.jpg
-rw-r--r-- 1 gohan gohan     587 May  9 14:11 multi_008_yellow_green_full_pipeline.json
```


## Assistant




**Tool Call: ls -la /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/images/**
Status: Completed

Terminal:
```
total 46096
drwxr-xr-x 1 gohan gohan    4096 May  9 14:36 .
drwxr-xr-x 1 gohan gohan    4096 May  9 14:17 ..
-rwxr--r-- 1 gohan gohan 7631011 May  9 12:16 015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG
-rwxr--r-- 1 gohan gohan 7343201 May  9 12:16 IMG20250416153020.jpg
-rwxr--r-- 1 gohan gohan 2214914 May  9 12:16 IMG20250416160821.jpg
-rwxr--r-- 1 gohan gohan 1179514 May  9 12:16 IMG_0422.jpg
-rwxr--r-- 1 gohan gohan 1365852 May  9 12:16 IMG_0771.jpg
-rwxr--r-- 1 gohan gohan 1143648 May  9 12:16 IMG_0787.jpg
-rwxr--r-- 1 gohan gohan 2124940 May  9 12:16 IMG_1536.jpg
-rwxr--r-- 1 gohan gohan 2587050 May  9 12:16 IMG_1537.jpg
-rwxr--r-- 1 gohan gohan 2115184 May  9 12:16 IMG_1548.jpg
-rwxr--r-- 1 gohan gohan 2227653 May  9 12:16 IMG_1571.jpg
-rwxr--r-- 1 gohan gohan  525869 May  9 12:16 M4sb29-2.jpg
-rwxr--r-- 1 gohan gohan  572345 May  9 12:16 M5sb24-10.jpg
-rwxr--r-- 1 gohan gohan  757846 May  9 12:16 N5G15-X1Y1.jpg
-rwxr--r-- 1 gohan gohan 6241020 May  9 12:16 multi_001_sunny_yellow.JPG
-rwxr--r-- 1 gohan gohan 9140524 May  9 12:16 multi_008_yellow_green.JPG
-rw-r--r-- 1 gohan gohan    1493 May  9 14:36 正解ラベル.csv
```


## Assistant

Let me read the rest of the design document and the key files.

**Tool Call: Read OCR/cclog/OCR-BBox-検出精度検証.md (2400 - 2799)**
Status: Completed

```
2400	OCR/src/activity_onnx_test.xml
2401	OCR/README.md
2402	OCR/scripts/ocr_eval.py
2403	OCR/results/eval_results.json
2404	OCR/cclog/OCR検証準備.md
2405	OCR/scripts/verify_detection_angle.py
2406	OCR/results/M4sb29-2_detection_angle.jpg
2407	OCR/results/M4sb29-2_detection_angle.json
2408	OCR/scripts/verify_full_pipeline.py
2409	OCR/results/M4sb29-2_full_pipeline.jpg
2410	OCR/results/M4sb29-2_full_pipeline.json
2411	OCR/cclog/OCR-BBox-角度検出.md
2412	OCR/scripts/batch_full_pipeline.py
2413	OCR/results/batch_full_pipeline/015_B15b30N-41__B15b30N-7A_45deg_NW_yellow_full_pipeline.jpg
2414	OCR/results/batch_full_pipeline/015_B15b30N-41__B15b30N-7A_45deg_NW_yellow_full_pipeline.json
2415	OCR/results/batch_full_pipeline/IMG20250416153020_full_pipeline.jpg
2416	OCR/results/batch_full_pipeline/IMG20250416153020_full_pipeline.json
2417	OCR/results/batch_full_pipeline/IMG20250416160821_full_pipeline.jpg
2418	OCR/results/batch_full_pipeline/IMG20250416160821_full_pipeline.json
2419	OCR/results/batch_full_pipeline/IMG_0422_full_pipeline.jpg
2420	OCR/results/batch_full_pipeline/IMG_0422_full_pipeline.json
2421	OCR/results/batch_full_pipeline/IMG_0771_full_pipeline.jpg
2422	OCR/results/batch_full_pipeline/IMG_0771_full_pipeline.json
2423	OCR/results/batch_full_pipeline/IMG_0787_full_pipeline.jpg
2424	OCR/results/batch_full_pipeline/IMG_0787_full_pipeline.json
2425	OCR/results/batch_full_pipeline/IMG_1536_full_pipeline.jpg
2426	OCR/results/batch_full_pipeline/IMG_1536_full_pipeline.json
2427	OCR/results/batch_full_pipeline/IMG_1537_full_pipeline.jpg
2428	OCR/results/batch_full_pipeline/IMG_1537_full_pipeline.json
2429	OCR/results/batch_full_pipeline/IMG_1548_full_pipeline.jpg
2430	OCR/results/batch_full_pipeline/IMG_1548_full_pipeline.json
2431	OCR/results/batch_full_pipeline/IMG_1571_full_pipeline.jpg
2432	OCR/results/batch_full_pipeline/IMG_1571_full_pipeline.json
2433	OCR/results/batch_full_pipeline/M4sb29-2_full_pipeline.jpg
2434	OCR/results/batch_full_pipeline/M4sb29-2_full_pipeline.json
2435	OCR/results/batch_full_pipeline/M5sb24-10_full_pipeline.jpg
2436	OCR/results/batch_full_pipeline/M5sb24-10_full_pipeline.json
2437	OCR/results/batch_full_pipeline/N5G15-X1Y1_full_pipeline.jpg
2438	OCR/results/batch_full_pipeline/N5G15-X1Y1_full_pipeline.json
2439	OCR/results/batch_full_pipeline/multi_001_sunny_yellow_full_pipeline.jpg
2440	OCR/results/batch_full_pipeline/multi_001_sunny_yellow_full_pipeline.json
2441	OCR/results/batch_full_pipeline/multi_008_yellow_green_full_pipeline.jpg
2442	OCR/results/batch_full_pipeline/multi_008_yellow_green_full_pipeline.json
2443	OCR/results/batch_full_pipeline/_summary.json
2444	OCR/使用可能文字.md
2445	OCR/scripts/PaddleOCR2-by-nogami.py
2446	OCR/images/正解ラベル.csv
2447	OCR/cclog/OCR-BBox-14枚に対しての検出.md
2448	
2449	## Assistant
2450	
2451	<thinking>
2452	Now I'll create the implementation plan with specific tasks. Let me first check the structure of the existing batch_full_pipeline.py to understand what we're building upon.
2453	
2454	</thinking>
2455	
2456	**Tool Call: Read OCR/scripts/batch_full_pipeline.py (1 - 50)**
2457	Status: Completed
2458	
2459	```
2460	1	#!/usr/bin/env python3
2461	2	"""
2462	3	PP-OCRv3 ONNX バッチ全パイプライン検証スクリプト
2463	4	OCR/images/ 内の全画像に対して検出・角度抽出・認識を実行し、
2464	5	results/batch_full_pipeline/ に結果を出力する
2465	6	"""
2466	7	import numpy as np
2467	8	import cv2
2468	9	import onnxruntime as ort
2469	10	from pathlib import Path
2470	11	import json
2471	12	import time
2472	13	import re
2473	14	import sys
2474	15	
2475	16	# === パス設定 ===
2476	17	SCRIPT_DIR = Path(__file__).parent
2477	18	OCR_DIR = SCRIPT_DIR.parent
2478	19	MODELS_DIR = OCR_DIR / "models"
2479	20	IMAGES_DIR = OCR_DIR / "images"
2480	21	RESULTS_DIR = OCR_DIR / "results" / "batch_full_pipeline"
2481	22	RESULTS_DIR.mkdir(exist_ok=True)
2482	23	
2483	24	DET_MODEL = MODELS_DIR / "det.onnx"
2484	25	REC_MODEL = MODELS_DIR / "en_PP-OCRv3_rec.onnx"
2485	26	DICT_FILE = MODELS_DIR / "en_PP-OCRv3_dict.txt"
2486	27	
2487	28	# 対象画像拡張子
2488	29	IMG_EXTS = {".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG"}
2489	30	
2490	31	def load_char_dict(dict_path):
2491	32	    chars = []
2492	33	    with open(dict_path, 'r', encoding='utf-8') as f:
2493	34	        for line in f:
2494	35	            chars.append(line.strip())
2495	36	    return chars
2496	37	
2497	38	def det_preprocess(img):
2498	39	    h, w = img.shape[:2]
2499	40	    scale = min(960 / h, 960 / w)
2500	41	    nh = int(h * scale) // 32 * 32
2501	42	    nw = int(w * scale) // 32 * 32
2502	43	    if nh == 0: nh = 32
2503	44	    if nw == 0: nw = 32
2504	45	    resized = cv2.resize(img, (nw, nh))
2505	46	    blob = cv2.dnn.blobFromImage(resized, 1.0, (nw, nh),
2506	47	                                  (123.675, 116.28, 103.53), swapRB=True)
2507	48	    mean = np.array([0.485, 0.456, 0.406]).reshape(1, 3, 1, 1)
2508	49	    std = np.array([0.229, 0.224, 0.225]).reshape(1, 3, 1, 1)
2509	50	    blob = (blob / 255.0 - mean) / std
2510	```
2511	
2512	## Assistant
2513	
2514	<thinking>
2515	Let me now read the 使用可能文字.md file to understand the allowed characters better, and then create the plan tasks.
2516	
2517	</thinking>
2518	
2519	**Tool Call: Read OCR/使用可能文字.md**
2520	Status: Completed
2521	
2522	```
2523	Wasted call — file unchanged since your last Read. Refer to that earlier tool_result instead.
2524	```
2525	
2526	## Assistant
2527	
2528	<thinking>
2529	Let me read the 正解ラベル.csv file to understand its structure.
2530	
2531	</thinking>
2532	
2533	**Tool Call: Read OCR/images/正解ラベル.csv**
2534	Status: Completed
2535	
2536	```
2537	Wasted call — file unchanged since your last Read. Refer to that earlier tool_result instead.
2538	```
2539	
2540	**Tool Call: Read OCR/scripts/verify_full_pipeline.py**
2541	Status: Completed
2542	
2543	```
2544	1	#!/usr/bin/env python3
2545	2	"""
2546	3	PP-OCRv3 ONNX 全パイプライン検証スクリプト
2547	4	検出・角度抽出・認識を統合し、結果を可視化
2548	5	"""
2549	6	import numpy as np
2550	7	import cv2
2551	8	import onnxruntime as ort
2552	9	from pathlib import Path
2553	10	import json
2554	11	import time
2555	12	import re
2556	13	
2557	14	# === パス設定 ===
2558	15	SCRIPT_DIR = Path(__file__).parent
2559	16	OCR_DIR = SCRIPT_DIR.parent
2560	17	MODELS_DIR = OCR_DIR / "models"
2561	18	IMAGES_DIR = OCR_DIR / "images"
2562	19	RESULTS_DIR = OCR_DIR / "results"
2563	20	RESULTS_DIR.mkdir(exist_ok=True)
2564	21	
2565	22	DET_MODEL = MODELS_DIR / "det.onnx"
2566	23	REC_MODEL = MODELS_DIR / "en_PP-OCRv3_rec.onnx"
2567	24	DICT_FILE = MODELS_DIR / "en_PP-OCRv3_dict.txt"
2568	25	
2569	26	def load_char_dict(dict_path):
2570	27	    chars = []
2571	28	    with open(dict_path, 'r', encoding='utf-8') as f:
2572	29	        for line in f:
2573	30	            chars.append(line.strip())
2574	31	    return chars
2575	32	
2576	33	def det_preprocess(img):
2577	34	    h, w = img.shape[:2]
2578	35	    scale = min(960 / h, 960 / w)
2579	36	    nh = int(h * scale) // 32 * 32
2580	37	    nw = int(w * scale) // 32 * 32
2581	38	    if nh == 0: nh = 32
2582	39	    if nw == 0: nw = 32
2583	40	    resized = cv2.resize(img, (nw, nh))
2584	41	    blob = cv2.dnn.blobFromImage(resized, 1.0, (nw, nh),
2585	42	                                  (123.675, 116.28, 103.53), swapRB=True)
2586	43	    mean = np.array([0.485, 0.456, 0.406]).reshape(1, 3, 1, 1)
2587	44	    std = np.array([0.229, 0.224, 0.225]).reshape(1, 3, 1, 1)
2588	45	    blob = (blob / 255.0 - mean) / std
2589	46	    return blob.astype(np.float32), scale, (h, w)
2590	47	
2591	48	def det_postprocess(output, orig_hw, det_thresh=0.3):
2592	49	    pred = output[0, 0]
2593	50	    binary = pred > det_thresh
2594	51	    binary_uint8 = (binary * 255).astype(np.uint8)
2595	52	    contours, _ = cv2.findContours(binary_uint8, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
2596	53	
2597	54	    h, w = pred.shape
2598	55	    orig_h, orig_w = orig_hw
2599	56	    scale_x = orig_w / w
2600	57	    scale_y = orig_h / h
2601	58	
2602	59	    boxes = []
2603	60	    angles = []
2604	61	    for contour in contours:
2605	62	        if len(contour) < 4:
2606	63	            continue
2607	64	        rect = cv2.minAreaRect(contour)
2608	65	        box = cv2.boxPoints(rect)
2609	66	        box_w = rect[1][0]
2610	67	        box_h = rect[1][1]
2611	68	        angle = rect[2]
2612	69	
2613	70	        if box_w < 5 or box_h < 5:
2614	71	            continue
2615	72	        box[:, 0] *= scale_x
2616	73	        box[:, 1] *= scale_y
2617	74	        box = box.astype(np.int32)
2618	75	        box[:, 0] = np.clip(box[:, 0], 0, orig_w - 1)
2619	76	        box[:, 1] = np.clip(box[:, 1], 0, orig_h - 1)
2620	77	        boxes.append(box)
2621	78	        angles.append(angle)
2622	79	    return boxes, angles
2623	80	
2624	81	def rec_preprocess(crop):
2625	82	    h, w = crop.shape[:2]
2626	83	    REC_H, REC_W = 48, 320
2627	84	    ratio = w / h
2628	85	    resized_w = int(ratio * REC_H)
2629	86	    if resized_w > REC_W:
2630	87	        resized_w = REC_W
2631	88	    if resized_w < 1:
2632	89	        resized_w = 1
2633	90	    resized = cv2.resize(crop, (resized_w, REC_H))
2634	91	    img = resized.astype(np.float32)
2635	92	    img = (img - 127.5) / 127.5
2636	93	    padded = np.full((REC_H, REC_W, 3), -1.0, dtype=np.float32)
2637	94	    padded[:, :resized_w, :] = img
2638	95	    blob = np.transpose(padded, (2, 0, 1))[np.newaxis, :, :, :].copy()
2639	96	    return blob
2640	97	
2641	98	def ctc_decode(logits, char_dict):
2642	99	    preds = np.argmax(logits, axis=-1)
2643	100	    results = []
2644	101	    for b in range(preds.shape[0]):
2645	102	        seq = preds[b]
2646	103	        result = []
2647	104	        prev = -1
2648	105	        for t in range(len(seq)):
2649	106	            c = seq[t]
2650	107	            if c == 0:
2651	108	                prev = -1
2652	109	                continue
2653	110	            if c == prev:
2654	111	                continue
2655	112	            dict_idx = c - 1
2656	113	            if 0 <= dict_idx < len(char_dict):
2657	114	                result.append(char_dict[dict_idx])
2658	115	            else:
2659	116	                result.append('?')
2660	117	            prev = c
2661	118	        results.append(''.join(result))
2662	119	    return results
2663	120	
2664	121	def recognize_region(crop, rec_sess, char_dict):
2665	122	    blob = rec_preprocess(crop)
2666	123	    input_name = rec_sess.get_inputs()[0].name
2667	124	    output_name = rec_sess.get_outputs()[0].name
2668	125	    output = rec_sess.run([output_name], {input_name: blob})[0]
2669	126	    texts = ctc_decode(output, char_dict)
2670	127	    return texts[0] if texts else ""
2671	128	
2672	129	def extract_products(text):
2673	130	    pattern = r'[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*'
2674	131	    matches = re.findall(pattern, text.upper())
2675	132	    return [m for m in matches if len(m) >= 3]
2676	133	
2677	134	def main():
2678	135	    print("Loading models...")
2679	136	    det_sess = ort.InferenceSession(str(DET_MODEL))
2680	137	    rec_sess = ort.InferenceSession(str(REC_MODEL))
2681	138	    char_dict = load_char_dict(DICT_FILE)
2682	139	
2683	140	    img_path = IMAGES_DIR / "M4sb29-2.jpg"
2684	141	    print(f"Processing image: {img_path}")
2685	142	
2686	143	    img = cv2.imread(str(img_path), cv2.IMREAD_COLOR)
2687	144	    if img is None:
2688	145	        print("Failed to load image")
2689	146	        return
2690	147	
2691	148	    # Detection
2692	149	    print("Running detection...")
2693	150	    det_input, det_scale, orig_hw = det_preprocess(img)
2694	151	    det_input_name = det_sess.get_inputs()[0].name
2695	152	    det_output_name = det_sess.get_outputs()[0].name
2696	153	    det_result = det_sess.run([det_output_name], {det_input_name: det_input})[0]
2697	154	    boxes, angles = det_postprocess(det_result, orig_hw, det_thresh=0.3)
2698	155	
2699	156	    print(f"Detected {len(boxes)} text regions")
2700	157	
2701	158	    # Recognition and result drawing
2702	159	    result_img = img.copy()
2703	160	    results = []
2704	161	    for i, (box, angle) in enumerate(zip(boxes, angles)):
2705	162	        # Expand box slightly to include more context
2706	163	        x_min, y_min = box[:, 0].min(), box[:, 1].min()
2707	164	        x_max, y_max = box[:, 0].max(), box[:, 1].max()
2708	165	        w = x_max - x_min
2709	166	        h = y_max - y_min
2710	167	        # Add 20% padding
2711	168	        pad_x = int(w * 0.2)
2712	169	        pad_y = int(h * 0.2)
2713	170	        x_min = max(0, x_min - pad_x)
2714	171	        y_min = max(0, y_min - pad_y)
2715	172	        x_max = min(img.shape[1], x_max + pad_x)
2716	173	        y_max = min(img.shape[0], y_max + pad_y)
2717	174	        if x_max <= x_min or y_max <= y_min:
2718	175	            continue
2719	176	        crop = img[y_min:y_max, x_min:x_max]
2720	177	
2721	178	        # Recognize
2722	179	        text = recognize_region(crop, rec_sess, char_dict)
2723	180	        products = extract_products(text)
2724	181	        print(f"Box {i}: angle = {angle:.1f}°, text = '{text}', products = {products}")
2725	182	
2726	183	        # Draw expanded box
2727	184	        cv2.polylines(result_img, [np.array([[x_min, y_min], [x_max, y_min], [x_max, y_max], [x_min, y_max]])], True, (0, 255, 0), 2)
2728	185	
2729	186	        # Draw angle and text
2730	187	        center_x = int((x_min + x_max) / 2)
2731	188	        center_y = int((y_min + y_max) / 2)
2732	189	        label = f"Angle: {angle:.1f}°"
2733	190	        if text:
2734	191	            label += f" | Text: {text}"
2735	192	        cv2.putText(result_img, label, (center_x - 100, center_y - 20),
2736	193	                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
2737	194	
2738	195	        results.append({
2739	196	            "box_index": i,
2740	197	            "angle_degree": float(angle),
2741	198	            "box_points": box.tolist(),
2742	199	            "expanded_box": [int(x_min), int(y_min), int(x_max), int(y_max)],
2743	200	            "recognized_text": text,
2744	201	            "product_codes": products
2745	202	        })
2746	203	
2747	204	    # Save result
2748	205	    output_path = RESULTS_DIR / "M4sb29-2_full_pipeline.jpg"
2749	206	    cv2.imwrite(str(output_path), result_img)
2750	207	    print(f"Result saved to: {output_path}")
2751	208	
2752	209	    # Save JSON
2753	210	    json_path = RESULTS_DIR / "M4sb29-2_full_pipeline.json"
2754	211	    with open(json_path, 'w', encoding='utf-8') as f:
2755	212	        json.dump({
2756	213	            "image": str(img_path.name),
2757	214	            "detections": results
2758	215	        }, f, indent=2, ensure_ascii=False)
2759	216	    print(f"Full data saved to: {json_path}")
2760	217	
2761	218	if __name__ == "__main__":
2762	219	    main()
2763	```
2764	
2765	**Tool Call: Edit docs/superpowers/plans/2026\-05\-09\-OCR\-BBox\-detection\-accuracy\-verification.md**
2766	Status: Failed
2767	
2768	```
2769	<tool_use_error>No changes to make: old_string and new_string are exactly the same.</tool_use_error>
2770	```
2771	
```

## Assistant




**Tool Call: Read OCR/images/正解ラベル.csv**
Status: Completed

```
1	"列1","Filename","色","Noise","傾き","光線","文字列個数","正解コード"
2	"1","IMG_0787.jpg
3	","黄色","","","明暗","1","BISb30N-16"
4	"2","M5sb24-10.jpg","水色","さび","","","1","M5sb24-10"
5	"3","IMG_1537.jpg","水色","さび","斜め","明暗","4","BISb40N-27
6	BISb40N-8
7	BISb40N-26
8	BISb40N-9"
9	"4","IMG_1548.jpg","水色","さび、ピントぼけ","垂直","","1","BISb40N-46"
10	"5","IMG_1571.jpg","黄色","","垂直","","2","BISb30N-13A
11	BISb30N-13"
12	"6","IMG_0422.jpg","黄色（そこだけ背景が白）","","少し斜め","","3","BISb30N-7A
13	BISb30N-4I
14	BISb30N-7A"
15	"7","IMG20250416153020.jpg","黄色、水色混合","","逆向き","","2","BISb30N-23
16	BISb30N-7"
17	"8","multi_008_yellow_green.JPG","黄色、水色混合","","","","5","BISb30N-7A
18	BISb30N-41
19	BISb30N-7A
20	BISb30N-53
21	BISb30N-37A"
22	"9","N5G15-X1Y1.jpg","水色","さび","","","1","N5G15-X1Y1"
23	"10","bundle/multi_001_sunny_yellow.JPG","黄色","","複数の向き","明暗","8","BISb30N-36
24	BISb30N-40
25	BISb30N-19
26	BISb30N-20
27	BISb30N-56
28	BISb30N-34
29	BISb30N-12
30	BISb30N-26"
31	"11","b015_B15b30N-41__B15b30N-7A_45deg_NW_yellow","黄色","","４５度","","4","BISb30N-7A
32	BISb30N-4I
33	BISb30N-7A
34	BISb30N-53"
35	"12","IIMG20250416160821.jpg","黄色","","","明暗混ざり","2","BISb30N-19
36	BISb30N-20"
37	"13","IMG_1536.jpg","水色","大きさ大小","","","2","BISb40N-23
38	BISb40N-15"
39	"14","IMG_0771.jpg","水色","色々な大きさ","","","3","BISb40N-23
40	BISb40N-15
41	BISb40N-27"
42	"15","M4sb29-2.jpg","水色","","","","1","M4sb29-2"
43	
```

## Assistant




**Tool Call: Read OCR/使用可能文字.md**
Status: Completed

```
1	| ■ | 使用不可文字 |   |
2	| --- | --- | --- |
3	|   | ! | 感嘆符、エクスクラメーションマーク |
4	|   | ? | 疑問符、クエスチョンマーク |
5	|   | ' | 引用符、（シングル）クォーテーション |
6	|   | " | 二重引用符、ダブルクォーテーション |
7	|   | # | シャープ、いげた |
8	|   | $ | ダラー、ドル、ドルマーク |
9	|   | \ | 円、円マーク |
10	|   | % | パーセント |
11	|   | & | アンパサンド、アンド |
12	|   | ( | かっこ、丸かっこ、パーレン |
13	|   | ) | かっこ、丸かっこ、パーレン |
14	|   | ＾ | カレット、キャレット、ハット |
15	|   | ~ | チルダ、チルダー |
16	|   | | | パイプ、パイプライン |
17	|   | @ | アットマーク |
18	|   | ` | バッククォート |
19	|   | [ | 大かっこ、ブラケット |
20	|   | ] | 大かっこ、ブラケット |
21	|   | { | 中かっこ、ブレイス |
22	|   | } | 中かっこ、ブレイス |
23	|   | * | アスタリスク |
24	|   | ; | セミコロン |
25	|   | : | コロン |
26	|   | , | カンマ、コンマ |
27	|   | . | ピリオド、ドット |
28	|   | < | 小なり、レスザン |
29	|   | > | 大なり、グレーターザン |
30	|   | / | スラッシュ |
31	|   | _ | アンダーバー、アンダースコア |
32	|   | = | イコール |
33	
34	| 使用不可範囲 | 使用可範囲 |
35	| --- | --- |
36	| 0x0000~0x001F | 0x0020 |
37	| 0x0021~0x002A | 0x002B |
38	| 0x002C | 0x002D~0x0039 |
39	| 0x003A~0x0040 | 0x0041~0x005A |
40	| 0x005B~0x005E | 0x005F |
41	| 0x0060 | 0x0061~0x007A |
42	| 0x007B~0x00FF |   |
43	
44	| U+ | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | A | B | C | D | E | F |
45	| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
46	| 0020 |   | ! | " | # | $ | % | & | ' | ( | ) | * | + | , | - | . | / |
47	| 0030 | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | : | ; | < | = | > | ? |
48	| 0040 | @ | A | B | C | D | E | F | G | H | I | J | K | L | M | N | O |
49	| 0050 | P | Q | R | S | T | U | V | W | X | Y | Z |   | \\ | ] | ^ | _ |
50	| 0060 | ` | a | b | c | d | e | f | g | h | i | j | k | l | m | n | o |
51	| 0070 | p | q | r | s | t | u | v | w | x | y | z | { | | | } | ~ | DEL |
52	
53	この表のうち使用可能な文字は以下です
54	
55	0020-0:
56	0020-B:+
57	0020-D:-
58	0020-E:.
59	0020-F:/
60	
61	数字 0–9（0030 行）
62	
63	- 0030-0:0
64	- 0030-1:1
65	- 0030-2:2
66	- 0030-3:3
67	- 0030-4:4
68	- 0030-5:5
69	- 0030-6:6
70	- 0030-7:7
71	- 0030-8:8
72	- 0030-9:9
73	
74	---
75	
76	英大文字 A–Z（0040, 0050 行）
77	
78	- 0040-1:A
79	- 0040-2:B
80	- 0040-3:C
81	- 0040-4:D
82	- 0040-5:E
83	- 0040-6:F
84	- 0040-7:G
85	- 0040-8:H
86	- 0040-9:I
87	- 0040-A:J
88	- 0040-B:K
89	- 0040-C:L
90	- 0040-D:M
91	- 0040-E:N
92	- 0040-F:O
93	- 0050-0:P
94	- 0050-1:Q
95	- 0050-2:R
96	- 0050-3:S
97	- 0050-4:T
98	- 0050-5:U
99	- 0050-6:V
100	- 0050-7:W
101	- 0050-8:X
102	- 0050-9:Y
103	- 0050-A:Z
104	
105	---
106	
107	英小文字 a–z（0060, 0070 行）
108	
109	- 0060-1:a
110	- 0060-2:b
111	- 0060-3:c
112	- 0060-4:d
113	- 0060-5:e
114	- 0060-6:f
115	- 0060-7:g
116	- 0060-8:h
117	- 0060-9:i
118	- 0060-A:j
119	- 0060-B:k
120	- 0060-C:l
121	- 0060-D:m
122	- 0060-E:n
123	- 0060-F:o
124	- 0070-0:p
125	- 0070-1:q
126	- 0070-2:r
127	- 0070-3:s
128	- 0070-4:t
129	- 0070-5:u
130	- 0070-6:v
131	- 0070-7:w
132	- 0070-8:x
133	- 0070-9:y
134	- 0070-A:z
135	
136	---
137	
138	アンダースコア `_`
139	
140	- 0050-F:_
141	
142	---
143	
```

## Assistant




**Tool Call: Read OCR/scripts/PaddleOCR2-by-nogami.py**
Status: Completed

```
1	import os
2	import cv2
3	import numpy as np
4	import onnxruntime as ort
5	
6	# --- 設定 ---
7	IMAGE_PATH = r"C:\Solution\resize14\M5sb24-10\M5sb24-10_resize.png"
8	MODEL_PATH = r"C:\Users\rinoa\Downloads\ppocrv3_rec.onnx"
9	DICT_PATH  = r"C:\Users\rinoa\Downloads\ppocrv3_dict.txt"
10	
11	def load_dict(path):
12	    with open(path, 'r', encoding='utf-8') as f:
13	        char_list = [line.strip("\r\n") for line in f.readlines() if line.strip("\r\n")]
14	    return ['blank'] + char_list
15	
16	def get_ocr_result(session, img, char_dict):
17	    """画像から文字と信頼度スコアを計算"""
18	    # モデルの入力に合わせてリサイズ
19	    target_h, target_w = 48, 320
20	    img_resized = cv2.resize(img, (target_w, target_h), interpolation=cv2.INTER_CUBIC)
21	    
22	    # 前処理
23	    img_proc = cv2.cvtColor(img_resized, cv2.COLOR_BGR2RGB).transpose((2, 0, 1))
24	    img_proc = (img_proc.astype('float32') / 255.0 - 0.5) / 0.5
25	    img_proc = np.expand_dims(img_proc, axis=0)
26	
27	    # 推論
28	    input_name = session.get_inputs()[0].name
29	    outputs = session.run(None, {input_name: img_proc})[0][0]
30	    
31	    # 確率計算
32	    exp_out = np.exp(outputs - np.max(outputs, axis=1, keepdims=True))
33	    probs = exp_out / np.sum(exp_out, axis=1, keepdims=True)
34	    
35	    preds_idx = np.argmax(probs, axis=1)
36	    preds_prob = np.max(probs, axis=1)
37	    
38	    result_text = []
39	    conf_scores = []
40	    last_idx = 0
41	    for i, idx in enumerate(preds_idx):
42	        if idx > 0 and idx != last_idx:
43	            if idx < len(char_dict):
44	                result_text.append(char_dict[idx])
45	                conf_scores.append(preds_prob[i])
46	        last_idx = idx
47	    
48	    avg_score = np.mean(conf_scores) if conf_scores else 0.0
49	    return "".join(result_text), avg_score, img_resized
50	
51	def main():
52	    print("\n" + "="*50)
53	    print("  4方向対応（0/90/180/270度）OCRテスト")
54	    print("="*50)
55	
56	    char_dict = load_dict(DICT_PATH)
57	    session = ort.InferenceSession(MODEL_PATH, providers=['CPUExecutionProvider'])
58	
59	    # 画像の読み込み
60	    n = np.fromfile(IMAGE_PATH, np.uint8)
61	    img = cv2.imdecode(n, cv2.IMREAD_COLOR)
62	    if img is None:
63	        print("画像が読み込めません。")
64	        return
65	
66	    # 4パターンの画像を生成してOCR実行
67	    # 辞書形式で結果を保存
68	    patterns = {
69	        "0度 (そのまま)": img,
70	        "90度 (時計回り)": cv2.rotate(img, cv2.ROTATE_90_CLOCKWISE),
71	        "180度 (逆さま)": cv2.rotate(img, cv2.ROTATE_180),
72	        "270度 (反時計回り)": cv2.rotate(img, cv2.ROTATE_90_COUNTERCLOCKWISE)
73	    }
74	
75	    best_text = ""
76	    best_score = -1.0
77	    best_orient = ""
78	    best_img = None
79	
80	    for label, rotated_img in patterns.items():
81	        text, score, res_img = get_ocr_result(session, rotated_img, char_dict)
82	        print(f"【{label}】")
83	        print(f"   認識: '{text}'  (信頼度: {score:.4f})")
84	        
85	        # スコアが一番高いものを記憶
86	        if score > best_score:
87	            best_score = score
88	            best_text = text
89	            best_orient = label
90	            best_img = res_img
91	
92	    print("\n" + "-"*40)
93	    print(f">>> 判定結果: {best_orient} を採用")
94	    print(f"最終認識文字列: {best_text}")
95	    print("-"*40 + "\n")
96	
97	    # 結果画像の表示
98	    cv2.imshow("Final Decision Image", cv2.resize(best_img, (640, 96)))
99	    print("どれかキーを押すと終了します。")
100	    cv2.waitKey(0)
101	    cv2.destroyAllWindows()
102	
103	if __name__ == "__main__":
104	    main()
```

## Assistant

Now let me read the end of the design document to see where it stopped:

**Tool Call: Read OCR/cclog/OCR-BBox-検出精度検証.md (2700 - 2999)**
Status: Completed

```
2700	157	
2701	158	    # Recognition and result drawing
2702	159	    result_img = img.copy()
2703	160	    results = []
2704	161	    for i, (box, angle) in enumerate(zip(boxes, angles)):
2705	162	        # Expand box slightly to include more context
2706	163	        x_min, y_min = box[:, 0].min(), box[:, 1].min()
2707	164	        x_max, y_max = box[:, 0].max(), box[:, 1].max()
2708	165	        w = x_max - x_min
2709	166	        h = y_max - y_min
2710	167	        # Add 20% padding
2711	168	        pad_x = int(w * 0.2)
2712	169	        pad_y = int(h * 0.2)
2713	170	        x_min = max(0, x_min - pad_x)
2714	171	        y_min = max(0, y_min - pad_y)
2715	172	        x_max = min(img.shape[1], x_max + pad_x)
2716	173	        y_max = min(img.shape[0], y_max + pad_y)
2717	174	        if x_max <= x_min or y_max <= y_min:
2718	175	            continue
2719	176	        crop = img[y_min:y_max, x_min:x_max]
2720	177	
2721	178	        # Recognize
2722	179	        text = recognize_region(crop, rec_sess, char_dict)
2723	180	        products = extract_products(text)
2724	181	        print(f"Box {i}: angle = {angle:.1f}°, text = '{text}', products = {products}")
2725	182	
2726	183	        # Draw expanded box
2727	184	        cv2.polylines(result_img, [np.array([[x_min, y_min], [x_max, y_min], [x_max, y_max], [x_min, y_max]])], True, (0, 255, 0), 2)
2728	185	
2729	186	        # Draw angle and text
2730	187	        center_x = int((x_min + x_max) / 2)
2731	188	        center_y = int((y_min + y_max) / 2)
2732	189	        label = f"Angle: {angle:.1f}°"
2733	190	        if text:
2734	191	            label += f" | Text: {text}"
2735	192	        cv2.putText(result_img, label, (center_x - 100, center_y - 20),
2736	193	                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
2737	194	
2738	195	        results.append({
2739	196	            "box_index": i,
2740	197	            "angle_degree": float(angle),
2741	198	            "box_points": box.tolist(),
2742	199	            "expanded_box": [int(x_min), int(y_min), int(x_max), int(y_max)],
2743	200	            "recognized_text": text,
2744	201	            "product_codes": products
2745	202	        })
2746	203	
2747	204	    # Save result
2748	205	    output_path = RESULTS_DIR / "M4sb29-2_full_pipeline.jpg"
2749	206	    cv2.imwrite(str(output_path), result_img)
2750	207	    print(f"Result saved to: {output_path}")
2751	208	
2752	209	    # Save JSON
2753	210	    json_path = RESULTS_DIR / "M4sb29-2_full_pipeline.json"
2754	211	    with open(json_path, 'w', encoding='utf-8') as f:
2755	212	        json.dump({
2756	213	            "image": str(img_path.name),
2757	214	            "detections": results
2758	215	        }, f, indent=2, ensure_ascii=False)
2759	216	    print(f"Full data saved to: {json_path}")
2760	217	
2761	218	if __name__ == "__main__":
2762	219	    main()
2763	```
2764	
2765	**Tool Call: Edit docs/superpowers/plans/2026\-05\-09\-OCR\-BBox\-detection\-accuracy\-verification.md**
2766	Status: Failed
2767	
2768	```
2769	<tool_use_error>No changes to make: old_string and new_string are exactly the same.</tool_use_error>
2770	```
2771	
```

## Assistant




**Tool Call: Read OCR/scripts/batch_full_pipeline.py**
Status: Completed

```
1	#!/usr/bin/env python3
2	"""
3	PP-OCRv3 ONNX バッチ全パイプライン検証スクリプト
4	OCR/images/ 内の全画像に対して検出・角度抽出・認識を実行し、
5	results/batch_full_pipeline/ に結果を出力する
6	"""
7	import numpy as np
8	import cv2
9	import onnxruntime as ort
10	from pathlib import Path
11	import json
12	import time
13	import re
14	import sys
15	
16	# === パス設定 ===
17	SCRIPT_DIR = Path(__file__).parent
18	OCR_DIR = SCRIPT_DIR.parent
19	MODELS_DIR = OCR_DIR / "models"
20	IMAGES_DIR = OCR_DIR / "images"
21	RESULTS_DIR = OCR_DIR / "results" / "batch_full_pipeline"
22	RESULTS_DIR.mkdir(exist_ok=True)
23	
24	DET_MODEL = MODELS_DIR / "det.onnx"
25	REC_MODEL = MODELS_DIR / "en_PP-OCRv3_rec.onnx"
26	DICT_FILE = MODELS_DIR / "en_PP-OCRv3_dict.txt"
27	
28	# 対象画像拡張子
29	IMG_EXTS = {".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG"}
30	
31	def load_char_dict(dict_path):
32	    chars = []
33	    with open(dict_path, 'r', encoding='utf-8') as f:
34	        for line in f:
35	            chars.append(line.strip())
36	    return chars
37	
38	def det_preprocess(img):
39	    h, w = img.shape[:2]
40	    scale = min(960 / h, 960 / w)
41	    nh = int(h * scale) // 32 * 32
42	    nw = int(w * scale) // 32 * 32
43	    if nh == 0: nh = 32
44	    if nw == 0: nw = 32
45	    resized = cv2.resize(img, (nw, nh))
46	    blob = cv2.dnn.blobFromImage(resized, 1.0, (nw, nh),
47	                                  (123.675, 116.28, 103.53), swapRB=True)
48	    mean = np.array([0.485, 0.456, 0.406]).reshape(1, 3, 1, 1)
49	    std = np.array([0.229, 0.224, 0.225]).reshape(1, 3, 1, 1)
50	    blob = (blob / 255.0 - mean) / std
51	    return blob.astype(np.float32), scale, (h, w)
52	
53	def det_postprocess(output, orig_hw, det_thresh=0.3):
54	    pred = output[0, 0]
55	    binary = pred > det_thresh
56	    binary_uint8 = (binary * 255).astype(np.uint8)
57	    contours, _ = cv2.findContours(binary_uint8, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
58	
59	    h, w = pred.shape
60	    orig_h, orig_w = orig_hw
61	    scale_x = orig_w / w
62	    scale_y = orig_h / h
63	
64	    boxes = []
65	    angles = []
66	    for contour in contours:
67	        if len(contour) < 4:
68	            continue
69	        rect = cv2.minAreaRect(contour)
70	        box = cv2.boxPoints(rect)
71	        box_w = rect[1][0]
72	        box_h = rect[1][1]
73	        angle = rect[2]
74	
75	        if box_w < 5 or box_h < 5:
76	            continue
77	        box[:, 0] *= scale_x
78	        box[:, 1] *= scale_y
79	        box = box.astype(np.int32)
80	        box[:, 0] = np.clip(box[:, 0], 0, orig_w - 1)
81	        box[:, 1] = np.clip(box[:, 1], 0, orig_h - 1)
82	        boxes.append(box)
83	        angles.append(angle)
84	    return boxes, angles
85	
86	def rec_preprocess(crop):
87	    h, w = crop.shape[:2]
88	    REC_H, REC_W = 48, 320
89	    ratio = w / h
90	    resized_w = int(ratio * REC_H)
91	    if resized_w > REC_W:
92	        resized_w = REC_W
93	    if resized_w < 1:
94	        resized_w = 1
95	    resized = cv2.resize(crop, (resized_w, REC_H))
96	    img = resized.astype(np.float32)
97	    img = (img - 127.5) / 127.5
98	    padded = np.full((REC_H, REC_W, 3), -1.0, dtype=np.float32)
99	    padded[:, :resized_w, :] = img
100	    blob = np.transpose(padded, (2, 0, 1))[np.newaxis, :, :, :].copy()
101	    return blob
102	
103	def ctc_decode(logits, char_dict):
104	    preds = np.argmax(logits, axis=-1)
105	    results = []
106	    for b in range(preds.shape[0]):
107	        seq = preds[b]
108	        result = []
109	        prev = -1
110	        for t in range(len(seq)):
111	            c = seq[t]
112	            if c == 0:
113	                prev = -1
114	                continue
115	            if c == prev:
116	                continue
117	            dict_idx = c - 1
118	            if 0 <= dict_idx < len(char_dict):
119	                result.append(char_dict[dict_idx])
120	            else:
121	                result.append('?')
122	            prev = c
123	        results.append(''.join(result))
124	    return results
125	
126	def recognize_region(crop, rec_sess, char_dict):
127	    blob = rec_preprocess(crop)
128	    input_name = rec_sess.get_inputs()[0].name
129	    output_name = rec_sess.get_outputs()[0].name
130	    output = rec_sess.run([output_name], {input_name: blob})[0]
131	    texts = ctc_decode(output, char_dict)
132	    return texts[0] if texts else ""
133	
134	def extract_products(text):
135	    pattern = r'[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*'
136	    matches = re.findall(pattern, text.upper())
137	    return [m for m in matches if len(m) >= 3]
138	
139	def process_image(img_path, det_sess, rec_sess, char_dict):
140	    img = cv2.imread(str(img_path), cv2.IMREAD_COLOR)
141	    if img is None:
142	        print(f"  FAILED to load image: {img_path}")
143	        return None
144	
145	    orig_h, orig_w = img.shape[:2]
146	
147	    # Detection
148	    det_input, det_scale, orig_hw = det_preprocess(img)
149	    det_input_name = det_sess.get_inputs()[0].name
150	    det_output_name = det_sess.get_outputs()[0].name
151	    det_result = det_sess.run([det_output_name], {det_input_name: det_input})[0]
152	    boxes, angles = det_postprocess(det_result, orig_hw, det_thresh=0.3)
153	
154	    # Recognition and result drawing
155	    result_img = img.copy()
156	    detections = []
157	    for i, (box, angle) in enumerate(zip(boxes, angles)):
158	        x_min, y_min = box[:, 0].min(), box[:, 1].min()
159	        x_max, y_max = box[:, 0].max(), box[:, 1].max()
160	        w = x_max - x_min
161	        h = y_max - y_min
162	        pad_x = int(w * 0.2)
163	        pad_y = int(h * 0.2)
164	        x_min = max(0, x_min - pad_x)
165	        y_min = max(0, y_min - pad_y)
166	        x_max = min(orig_w, x_max + pad_x)
167	        y_max = min(orig_h, y_max + pad_y)
168	        if x_max <= x_min or y_max <= y_min:
169	            continue
170	        crop = img[y_min:y_max, x_min:x_max]
171	
172	        text = recognize_region(crop, rec_sess, char_dict)
173	        products = extract_products(text)
174	
175	        # Draw expanded box
176	        cv2.polylines(result_img, [np.array([[x_min, y_min], [x_max, y_min], [x_max, y_max], [x_min, y_max]])], True, (0, 255, 0), 2)
177	
178	        # Draw angle and text
179	        center_x = int((x_min + x_max) / 2)
180	        center_y = int((y_min + y_max) / 2)
181	        label = f"{angle:.1f}"
182	        if text:
183	            label += f"|{text}"
184	        cv2.putText(result_img, label, (center_x - 100, center_y - 20),
185	                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
186	
187	        detections.append({
188	            "box_index": i,
189	            "angle_degree": float(angle),
190	            "box_points": box.tolist(),
191	            "expanded_box": [int(x_min), int(y_min), int(x_max), int(y_max)],
192	            "recognized_text": text,
193	            "product_codes": products
194	        })
195	
196	    # Save result image
197	    out_img_path = RESULTS_DIR / f"{img_path.stem}_full_pipeline.jpg"
198	    cv2.imwrite(str(out_img_path), result_img)
199	
200	    # Save JSON
201	    out_json_path = RESULTS_DIR / f"{img_path.stem}_full_pipeline.json"
202	    result_data = {
203	        "image": img_path.name,
204	        "image_size": [orig_w, orig_h],
205	        "num_detections": len(detections),
206	        "detections": detections
207	    }
208	    with open(out_json_path, 'w', encoding='utf-8') as f:
209	        json.dump(result_data, f, indent=2, ensure_ascii=False)
210	
211	    return result_data
212	
213	def main():
214	    # Collect image files
215	    img_files = sorted([f for f in IMAGES_DIR.iterdir()
216	                        if f.is_file() and f.suffix in IMG_EXTS])
217	    print(f"Found {len(img_files)} images in {IMAGES_DIR}")
218	
219	    if not img_files:
220	        print("No images found. Exiting.")
221	        sys.exit(1)
222	
223	    # Load models once
224	    print("Loading models...")
225	    det_sess = ort.InferenceSession(str(DET_MODEL))
226	    rec_sess = ort.InferenceSession(str(REC_MODEL))
227	    char_dict = load_char_dict(DICT_FILE)
228	    print("Models loaded.")
229	
230	    # Process all images
231	    summary = []
232	    total_start = time.time()
233	
234	    for idx, img_path in enumerate(img_files, 1):
235	        print(f"\n[{idx}/{len(img_files)}] Processing: {img_path.name}")
236	        t0 = time.time()
237	        result = process_image(img_path, det_sess, rec_sess, char_dict)
238	        elapsed = time.time() - t0
239	
240	        if result is None:
241	            summary.append({
242	                "image": img_path.name,
243	                "status": "FAILED",
244	                "num_detections": 0,
245	                "time_seconds": round(elapsed, 2)
246	            })
247	        else:
248	            num_det = result["num_detections"]
249	            all_products = []
250	            for d in result["detections"]:
251	                all_products.extend(d["product_codes"])
252	            print(f"  -> {num_det} regions, products: {all_products}, time: {elapsed:.2f}s")
253	            summary.append({
254	                "image": img_path.name,
255	                "status": "OK",
256	                "num_detections": num_det,
257	                "product_codes": all_products,
258	                "time_seconds": round(elapsed, 2)
259	            })
260	
261	    total_elapsed = time.time() - total_start
262	
263	    # Save summary
264	    summary_data = {
265	        "total_images": len(img_files),
266	        "total_time_seconds": round(total_elapsed, 2),
267	        "results": summary
268	    }
269	    summary_path = RESULTS_DIR / "_summary.json"
270	    with open(summary_path, 'w', encoding='utf-8') as f:
271	        json.dump(summary_data, f, indent=2, ensure_ascii=False)
272	
273	    # Print summary
274	    print(f"\n{'='*60}")
275	    print(f"BATCH RESULTS SUMMARY")
276	    print(f"{'='*60}")
277	    print(f"Total images: {len(img_files)}")
278	    print(f"Total time: {total_elapsed:.2f}s")
279	    print(f"Output directory: {RESULTS_DIR}")
280	    print(f"{'='*60}")
281	    for s in summary:
282	        status = s['status']
283	        dets = s.get('num_detections', 0)
284	        products = s.get('product_codes', [])
285	        t = s['time_seconds']
286	        print(f"  {s['image']:40s} | {status:6s} | {dets:3d} dets | products: {products} | {t:.2f}s")
287	    print(f"{'='*60}")
288	    print(f"Summary saved to: {summary_path}")
289	
290	if __name__ == "__main__":
291	    main()
292	
```

## Assistant

Now let me read the last part of the design doc to see the exact error:

**Tool Call: Read OCR/cclog/OCR-BBox-検出精度検証.md (2526 - 2725)**
Status: Completed

```
2526	## Assistant
2527	
2528	<thinking>
2529	Let me read the 正解ラベル.csv file to understand its structure.
2530	
2531	</thinking>
2532	
2533	**Tool Call: Read OCR/images/正解ラベル.csv**
2534	Status: Completed
2535	
2536	```
2537	Wasted call — file unchanged since your last Read. Refer to that earlier tool_result instead.
2538	```
2539	
2540	**Tool Call: Read OCR/scripts/verify_full_pipeline.py**
2541	Status: Completed
2542	
2543	```
2544	1	#!/usr/bin/env python3
2545	2	"""
2546	3	PP-OCRv3 ONNX 全パイプライン検証スクリプト
2547	4	検出・角度抽出・認識を統合し、結果を可視化
2548	5	"""
2549	6	import numpy as np
2550	7	import cv2
2551	8	import onnxruntime as ort
2552	9	from pathlib import Path
2553	10	import json
2554	11	import time
2555	12	import re
2556	13	
2557	14	# === パス設定 ===
2558	15	SCRIPT_DIR = Path(__file__).parent
2559	16	OCR_DIR = SCRIPT_DIR.parent
2560	17	MODELS_DIR = OCR_DIR / "models"
2561	18	IMAGES_DIR = OCR_DIR / "images"
2562	19	RESULTS_DIR = OCR_DIR / "results"
2563	20	RESULTS_DIR.mkdir(exist_ok=True)
2564	21	
2565	22	DET_MODEL = MODELS_DIR / "det.onnx"
2566	23	REC_MODEL = MODELS_DIR / "en_PP-OCRv3_rec.onnx"
2567	24	DICT_FILE = MODELS_DIR / "en_PP-OCRv3_dict.txt"
2568	25	
2569	26	def load_char_dict(dict_path):
2570	27	    chars = []
2571	28	    with open(dict_path, 'r', encoding='utf-8') as f:
2572	29	        for line in f:
2573	30	            chars.append(line.strip())
2574	31	    return chars
2575	32	
2576	33	def det_preprocess(img):
2577	34	    h, w = img.shape[:2]
2578	35	    scale = min(960 / h, 960 / w)
2579	36	    nh = int(h * scale) // 32 * 32
2580	37	    nw = int(w * scale) // 32 * 32
2581	38	    if nh == 0: nh = 32
2582	39	    if nw == 0: nw = 32
2583	40	    resized = cv2.resize(img, (nw, nh))
2584	41	    blob = cv2.dnn.blobFromImage(resized, 1.0, (nw, nh),
2585	42	                                  (123.675, 116.28, 103.53), swapRB=True)
2586	43	    mean = np.array([0.485, 0.456, 0.406]).reshape(1, 3, 1, 1)
2587	44	    std = np.array([0.229, 0.224, 0.225]).reshape(1, 3, 1, 1)
2588	45	    blob = (blob / 255.0 - mean) / std
2589	46	    return blob.astype(np.float32), scale, (h, w)
2590	47	
2591	48	def det_postprocess(output, orig_hw, det_thresh=0.3):
2592	49	    pred = output[0, 0]
2593	50	    binary = pred > det_thresh
2594	51	    binary_uint8 = (binary * 255).astype(np.uint8)
2595	52	    contours, _ = cv2.findContours(binary_uint8, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
2596	53	
2597	54	    h, w = pred.shape
2598	55	    orig_h, orig_w = orig_hw
2599	56	    scale_x = orig_w / w
2600	57	    scale_y = orig_h / h
2601	58	
2602	59	    boxes = []
2603	60	    angles = []
2604	61	    for contour in contours:
2605	62	        if len(contour) < 4:
2606	63	            continue
2607	64	        rect = cv2.minAreaRect(contour)
2608	65	        box = cv2.boxPoints(rect)
2609	66	        box_w = rect[1][0]
2610	67	        box_h = rect[1][1]
2611	68	        angle = rect[2]
2612	69	
2613	70	        if box_w < 5 or box_h < 5:
2614	71	            continue
2615	72	        box[:, 0] *= scale_x
2616	73	        box[:, 1] *= scale_y
2617	74	        box = box.astype(np.int32)
2618	75	        box[:, 0] = np.clip(box[:, 0], 0, orig_w - 1)
2619	76	        box[:, 1] = np.clip(box[:, 1], 0, orig_h - 1)
2620	77	        boxes.append(box)
2621	78	        angles.append(angle)
2622	79	    return boxes, angles
2623	80	
2624	81	def rec_preprocess(crop):
2625	82	    h, w = crop.shape[:2]
2626	83	    REC_H, REC_W = 48, 320
2627	84	    ratio = w / h
2628	85	    resized_w = int(ratio * REC_H)
2629	86	    if resized_w > REC_W:
2630	87	        resized_w = REC_W
2631	88	    if resized_w < 1:
2632	89	        resized_w = 1
2633	90	    resized = cv2.resize(crop, (resized_w, REC_H))
2634	91	    img = resized.astype(np.float32)
2635	92	    img = (img - 127.5) / 127.5
2636	93	    padded = np.full((REC_H, REC_W, 3), -1.0, dtype=np.float32)
2637	94	    padded[:, :resized_w, :] = img
2638	95	    blob = np.transpose(padded, (2, 0, 1))[np.newaxis, :, :, :].copy()
2639	96	    return blob
2640	97	
2641	98	def ctc_decode(logits, char_dict):
2642	99	    preds = np.argmax(logits, axis=-1)
2643	100	    results = []
2644	101	    for b in range(preds.shape[0]):
2645	102	        seq = preds[b]
2646	103	        result = []
2647	104	        prev = -1
2648	105	        for t in range(len(seq)):
2649	106	            c = seq[t]
2650	107	            if c == 0:
2651	108	                prev = -1
2652	109	                continue
2653	110	            if c == prev:
2654	111	                continue
2655	112	            dict_idx = c - 1
2656	113	            if 0 <= dict_idx < len(char_dict):
2657	114	                result.append(char_dict[dict_idx])
2658	115	            else:
2659	116	                result.append('?')
2660	117	            prev = c
2661	118	        results.append(''.join(result))
2662	119	    return results
2663	120	
2664	121	def recognize_region(crop, rec_sess, char_dict):
2665	122	    blob = rec_preprocess(crop)
2666	123	    input_name = rec_sess.get_inputs()[0].name
2667	124	    output_name = rec_sess.get_outputs()[0].name
2668	125	    output = rec_sess.run([output_name], {input_name: blob})[0]
2669	126	    texts = ctc_decode(output, char_dict)
2670	127	    return texts[0] if texts else ""
2671	128	
2672	129	def extract_products(text):
2673	130	    pattern = r'[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*'
2674	131	    matches = re.findall(pattern, text.upper())
2675	132	    return [m for m in matches if len(m) >= 3]
2676	133	
2677	134	def main():
2678	135	    print("Loading models...")
2679	136	    det_sess = ort.InferenceSession(str(DET_MODEL))
2680	137	    rec_sess = ort.InferenceSession(str(REC_MODEL))
2681	138	    char_dict = load_char_dict(DICT_FILE)
2682	139	
2683	140	    img_path = IMAGES_DIR / "M4sb29-2.jpg"
2684	141	    print(f"Processing image: {img_path}")
2685	142	
2686	143	    img = cv2.imread(str(img_path), cv2.IMREAD_COLOR)
2687	144	    if img is None:
2688	145	        print("Failed to load image")
2689	146	        return
2690	147	
2691	148	    # Detection
2692	149	    print("Running detection...")
2693	150	    det_input, det_scale, orig_hw = det_preprocess(img)
2694	151	    det_input_name = det_sess.get_inputs()[0].name
2695	152	    det_output_name = det_sess.get_outputs()[0].name
2696	153	    det_result = det_sess.run([det_output_name], {det_input_name: det_input})[0]
2697	154	    boxes, angles = det_postprocess(det_result, orig_hw, det_thresh=0.3)
2698	155	
2699	156	    print(f"Detected {len(boxes)} text regions")
2700	157	
2701	158	    # Recognition and result drawing
2702	159	    result_img = img.copy()
2703	160	    results = []
2704	161	    for i, (box, angle) in enumerate(zip(boxes, angles)):
2705	162	        # Expand box slightly to include more context
2706	163	        x_min, y_min = box[:, 0].min(), box[:, 1].min()
2707	164	        x_max, y_max = box[:, 0].max(), box[:, 1].max()
2708	165	        w = x_max - x_min
2709	166	        h = y_max - y_min
2710	167	        # Add 20% padding
2711	168	        pad_x = int(w * 0.2)
2712	169	        pad_y = int(h * 0.2)
2713	170	        x_min = max(0, x_min - pad_x)
2714	171	        y_min = max(0, y_min - pad_y)
2715	172	        x_max = min(img.shape[1], x_max + pad_x)
2716	173	        y_max = min(img.shape[0], y_max + pad_y)
2717	174	        if x_max <= x_min or y_max <= y_min:
2718	175	            continue
2719	176	        crop = img[y_min:y_max, x_min:x_max]
2720	177	
2721	178	        # Recognize
2722	179	        text = recognize_region(crop, rec_sess, char_dict)
2723	180	        products = extract_products(text)
2724	181	        print(f"Box {i}: angle = {angle:.1f}°, text = '{text}', products = {products}")
2725	182	
```

## Assistant

I can see the doc ends at the `verify_full_pipeline.py` content. The previous session was trying to create a plan file and then stopped. Let me check if there's a plan file already:

**Tool Call: ls -la /mnt/c/Users/soyak/StudioProjects/crossvision/docs/superpowers/plans/ 2>/dev/null || echo "No plans dir"**
Status: Completed

Terminal:
```
total 4
drwxr-xr-x 1 gohan gohan 4096 May  9 15:58 .
drwxr-xr-x 1 gohan gohan 4096 May  9 15:58 ..
-rw-r--r-- 1 gohan gohan  811 May  9 15:58 2026-05-09-OCR-BBox-detection-accuracy-verification.md

```


## Assistant




**Tool Call: ls -la /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/models/**
Status: Completed

Terminal:
```
total 111140
drwxr-xr-x 1 gohan gohan     4096 May  9 12:16 .
drwxr-xr-x 1 gohan gohan     4096 May  9 14:17 ..
-rwxr--r-- 1 gohan gohan 88030804 May  9 12:16 det.onnx
-rwxr--r-- 1 gohan gohan     1416 May  9 12:16 dict.txt
-rwxr--r-- 1 gohan gohan      190 May  9 12:16 en_PP-OCRv3_dict.txt
-rwxr--r-- 1 gohan gohan  8968230 May  9 12:16 en_PP-OCRv3_rec.onnx
-rwxr--r-- 1 gohan gohan  7830888 May  9 12:16 pnnx_english.onnx
-rwxr--r-- 1 gohan gohan      192 May  9 12:16 ppocrv3_dict.txt
-rwxr--r-- 1 gohan gohan  8967018 May  9 12:16 ppocrv3_rec.onnx
```


**Tool Call: Read docs/superpowers/plans/2026-05-09-OCR-BBox-detection-accuracy-verification.md**
Status: Completed

```
1	# OCR-BBox Detection Accuracy Verification Implementation Plan
2	
3	> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
4	
5	**Goal:** Create a verification script that processes 15 OCR images, filters recognized text by allowed characters, compares with correct labels, and calculates accuracy metrics.
6	
7	**Architecture:** We'll create a new verification script that builds upon the existing batch_full_pipeline.py core logic, adding character filtering, label comparison, and accuracy calculation. The script will output results to results/batch_full_pipeline2/.
8	
9	**Tech Stack:** Python, OpenCV, ONNX Runtime, NumPy, Pathlib, JSON, CSV, Regex
10	
11	---
```

## Assistant

Good. Now I have the full picture. Let me understand the requirements and create the `batch_full_pipeline2.py` script. The previous session stopped while trying to create the plan. Let me now implement the actual script.

Based on my analysis:
1. **Input**: 15 images in `OCR/images/`, correct labels in `正解ラベル.csv`, allowed chars in `使用可能文字.md`
2. **Pipeline**: det+rec (existing), but needs rotated bounding box crop + angle-aware recognition
3. **Output**: `results/batch_full_pipeline2/` with per-image results + accuracy metrics
4. **Metrics**: exact match rate, partial match rate per image + overall summaryNow let me check the dict file to understand the character set:

**Tool Call: Read OCR/models/en_PP-OCRv3_dict.txt**
Status: Completed

```
1	0
2	1
3	2
4	3
5	4
6	5
7	6
8	7
9	8
10	9
11	:
12	;
13	<
14	=
15	>
16	?
17	@
18	A
19	B
20	C
21	D
22	E
23	F
24	G
25	H
26	I
27	J
28	K
29	L
30	M
31	N
32	O
33	P
34	Q
35	R
36	S
37	T
38	U
39	V
40	W
41	X
42	Y
43	Z
44	[
45	\
46	]
47	^
48	_
49	`
50	a
51	b
52	c
53	d
54	e
55	f
56	g
57	h
58	i
59	j
60	k
61	l
62	m
63	n
64	o
65	p
66	q
67	r
68	s
69	t
70	u
71	v
72	w
73	x
74	y
75	z
76	{
77	|
78	}
79	~
80	!
81	"
82	#
83	$
84	%
85	&
86	'
87	(
88	)
89	*
90	+
91	,
92	-
93	.
94	/
95	 
96	
```

## Assistant

Now I have everything I need. Let me create the `batch_full_pipeline2.py` script. Key design decisions:

1. **Rotated crop**: Use `cv2.getRotationMatrix2D` + `cv2.warpAffine` to rotate the image based on the detected angle, then crop the text region properly
2. **Allowed chars filter**: After recognition, filter out characters not in the allowed set (from `使用可能文字.md`: alphanumeric, `-`, `.`, `/`, `+`, `_`, space)
3. **Label comparison**: Parse `正解ラベル.csv`, match by filename, compute exact/partial match rates
4. **Output**: `results/batch_full_pipeline2/` with per-image JSON + JPG + summary

**Tool Call: Write**
Status: Pending

**Tool Call: Write OCR/scripts/batch\_full\_pipeline2.py**
Status: Completed

Diff: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/batch_full_pipeline2.py
```
#!/usr/bin/env python3
"""
PP-OCRv3 ONNX バッチ全パイプライン検証スクリプト v2
- 回転バウンディングボックス対応（傾きテキストの正確なクロップ）
- 使用可能文字フィルタリング
- 正解ラベルとの照合（完全一致・部分一致の計算）
- 結果を results/batch_full_pipeline2/ に出力
"""
import numpy as np
import cv2
import onnxruntime as ort
from pathlib import Path
import json
import time
import re
import sys
import csv
from collections import defaultdict

# === パス設定 ===
SCRIPT_DIR = Path(__file__).parent
OCR_DIR = SCRIPT_DIR.parent
MODELS_DIR = OCR_DIR / "models"
IMAGES_DIR = OCR_DIR / "images"
RESULTS_DIR = OCR_DIR / "results" / "batch_full_pipeline2"
RESULTS_DIR.mkdir(exist_ok=True)

DET_MODEL = MODELS_DIR / "det.onnx"
REC_MODEL = MODELS_DIR / "en_PP-OCRv3_rec.onnx"
DICT_FILE = MODELS_DIR / "en_PP-OCRv3_dict.txt"
LABEL_CSV = IMAGES_DIR / "正解ラベル.csv"

# 対象画像拡張子
IMG_EXTS = {".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG"}

# === 使用可能文字セット ===
# 英大文字 A-Z, 英小文字 a-z, 数字 0-9, ハイフン-, スラッシュ/, プラス+, ピリオド., アンダースコア_, スペース
ALLOWED_CHARS = set(
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
    "0123456789"
    "-+/._ "
)


def filter_allowed(text):
    """使用可能文字のみを残す"""
    return "".join(c for c in text if c in ALLOWED_CHARS)


def load_char_dict(dict_path):
    chars = []
    with open(dict_path, 'r', encoding='utf-8') as f:
        for line in f:
            chars.append(line.strip())
    return chars


def load_labels(csv_path):
    """正解ラベルCSVを読み込む。ファイル名→正解コードリストの辞書を返す"""
    labels = {}
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        header = next(reader)  # skip header
        for row in reader:
            if len(row) < 8:
                continue
            filename_raw = row[1].strip()
            # ファイル名の末尾に.jpg等が含まれている場合と含まれていない場合がある
            # 改行が入っている場合もあるので除去
            filename = filename_raw.strip().replace('\n', '').replace('\r', '')
            # 拡張子がない場合は .jpg を補完
            if not Path(filename).suffix:
                filename += ".jpg"
            correct_codes = row[7].strip()
            # 正解コードは改行区切りで複数ある場合がある
            codes = [c.strip() for c in correct_codes.split('\n') if c.strip()]
            labels[filename] = codes
    return labels


def det_preprocess(img):
    h, w = img.shape[:2]
    scale = min(960 / h, 960 / w)
    nh = int(h * scale) // 32 * 32
    nw = int(w * scale) // 32 * 32
    if nh == 0: nh = 32
    if nw == 0: nw = 32
    resized = cv2.resize(img, (nw, nh))
    blob = cv2.dnn.blobFromImage(resized, 1.0, (nw, nh),
                                  (123.675, 116.28, 103.53), swapRB=True)
    mean = np.array([0.485, 0.456, 0.406]).reshape(1, 3, 1, 1)
    std = np.array([0.229, 0.224, 0.225]).reshape(1, 3, 1, 1)
    blob = (blob / 255.0 - mean) / std
    return blob.astype(np.float32), scale, (h, w)


def det_postprocess(output, orig_hw, det_thresh=0.3):
    pred = output[0, 0]
    binary = pred > det_thresh
    binary_uint8 = (binary * 255).astype(np.uint8)
    contours, _ = cv2.findContours(binary_uint8, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)

    h, w = pred.shape
    orig_h, orig_w = orig_hw
    scale_x = orig_w / w
    scale_y = orig_h / h

    boxes = []
    angles = []
    scores = []
    for contour in contours:
        if len(contour) < 4:
            continue
        rect = cv2.minAreaRect(contour)
        box = cv2.boxPoints(rect)
        box_w = rect[1][0]
        box_h = rect[1][1]
        angle = rect[2]

        if box_w < 5 or box_h < 5:
            continue

        # スコア計算: その領域内の平均スコア
        mask = np.zeros((h, w), dtype=np.uint8)
        contour_scaled = contour.copy()
        contour_scaled[:, :, 0] = np.clip(contour_scaled[:, :, 0] / scale_x, 0, w - 1).astype(np.int32)
        contour_scaled[:, :, 1] = np.clip(contour_scaled[:, :, 1] / scale_y, 0, h - 1).astype(np.int32)
        cv2.fillPoly(mask, [contour_scaled], 1)
        score = float(np.mean(pred[mask > 0])) if np.sum(mask) > 0 else 0.0

        box[:, 0] *= scale_x
        box[:, 1] *= scale_y
        box = box.astype(np.int32)
        box[:, 0] = np.clip(box[:, 0], 0, orig_w - 1)
        box[:, 1] = np.clip(box[:, 1], 0, orig_h - 1)
        boxes.append(box)
        angles.append(angle)
        scores.append(score)
    return boxes, angles, scores


def crop_rotated(img, box, angle, padding=4):
    """
    回転バウンディングボックスからテキスト領域を正確にクロップする。
    1. バウンディングボックスの最小外接矩形を計算
    2. 画像全体を回転させてテキストを水平にする
    3. 水平なテキスト領域をクロップ
    """
    # box: 4点の座標 (cv2.boxPoints の出力)
    # angle: minAreaRect の角度

    # バウンディングボックスの幅と高さを計算
    rect = cv2.minAreaRect(box.astype(np.int32))
    center = rect[0]
    w, h = rect[1]
    angle_deg = rect[2]

    if w < 1 or h < 1:
        return None

    # 幅と高さが逆になっている場合があるので修正
    # minAreaRect は w < h のとき角度が変わるため
    if w < h:
        w, h = h, w
        angle_deg = angle_deg + 90

    # パディング追加
    w_pad = int(w) + padding * 2
    h_pad = int(h) + padding * 2

    # 画像全体を回転
    M = cv2.getRotationMatrix2D(center, angle_deg, 1.0)
    rotated = cv2.warpAffine(img, M, (img.shape[1], img.shape[0]),
                             flags=cv2.INTER_CUBIC,
                             borderMode=cv2.BORDER_REPLICATE)

    # 回転後の中心からクロップ
    cx, cy = int(center[0]), int(center[1])
    x1 = max(0, cx - w_pad // 2)
    y1 = max(0, cy - h_pad // 2)
    x2 = min(img.shape[1], cx + w_pad // 2)
    y2 = min(img.shape[0], cy + h_pad // 2)

    if x2 <= x1 or y2 <= y1:
        return None

    crop = rotated[y1:y2, x1:x2]
    if crop.size == 0:
        return None
    return crop


def rec_preprocess(crop):
    h, w = crop.shape[:2]
    REC_H, REC_W = 48, 320
    ratio = w / h
    resized_w = int(ratio * REC_H)
    if resized_w > REC_W:
        resized_w = REC_W
    if resized_w < 1:
        resized_w = 1
    resized = cv2.resize(crop, (resized_w, REC_H))
    img = resized.astype(np.float32)
    img = (img - 127.5) / 127.5
    padded = np.full((REC_H, REC_W, 3), -1.0, dtype=np.float32)
    padded[:, :resized_w, :] = img
    blob = np.transpose(padded, (2, 0, 1))[np.newaxis, :, :, :].copy()
    return blob


def ctc_decode(logits, char_dict):
    preds = np.argmax(logits, axis=-1)
    results = []
    confidences = []
    for b in range(preds.shape[0]):
        seq = preds[b]
        result = []
        confs = []
        prev = -1
        for t in range(len(seq)):
            c = seq[t]
            if c == 0:
                prev = -1
                continue
            if c == prev:
                continue
            dict_idx = c - 1
            if 0 <= dict_idx < len(char_dict):
                result.append(char_dict[dict_idx])
                # 信頼度 = 最大確率
                prob = float(np.exp(logits[b, t, c]) / np.sum(np.exp(logits[b, t])))
                confs.append(prob)
            else:
                result.append('?')
                confs.append(0.0)
            prev = c
        results.append(''.join(result))
        avg_conf = np.mean(confs) if confs else 0.0
        confidences.append(avg_conf)
    return results, confidences


def recognize_region(crop, rec_sess, char_dict):
    blob = rec_preprocess(crop)
    input_name = rec_sess.get_inputs()[0].name
    output_name = rec_sess.get_outputs()[0].name
    output = rec_sess.run([output_name], {input_name: blob})[0]
    texts, confs = ctc_decode(output, char_dict)
    return (texts[0] if texts else ""), (confs[0] if confs else 0.0)


def extract_products(text):
    """製品コードらしい文字列を抽出（英数字とハイフンの組み合わせ）"""
    pattern = r'[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*'
    matches = re.findall(pattern, text.upper())
    return [m for m in matches if len(m) >= 3]


def normalize_code(code):
    """コードを正規化して比較用に"""
    return code.strip().upper()


def compute_match(recognized_codes, correct_codes):
    """
    認識結果と正解ラベルの一致率を計算
    - 完全一致: 認識コード == 正解コード
    - 部分一致: 正解コードの一部が認識コードに含まれる（またはその逆）
    """
    if not correct_codes:
        return {
            "exact_matches": [],
            "partial_matches": [],
            "misses": [],
            "extra": [],
            "exact_rate": 0.0,
            "partial_rate": 0.0
        }

    rec_norm = [normalize_code(c) for c in recognized_codes]
    cor_norm = [normalize_code(c) for c in correct_codes]

    exact_matches = []
    partial_matches = []
    matched_rec = set()
    matched_cor = set()

    # 完全一致チェック
    for i, r in enumerate(rec_norm):
        for j, c in enumerate(cor_norm):
            if j in matched_cor:
                continue
            if r == c:
                exact_matches.append({"recognized": recognized_codes[i], "correct": correct_codes[j]})
                matched_rec.add(i)
                matched_cor.add(j)
                break

    # 部分一致チェック（完全一致しなかったもの同士）
    for i, r in enumerate(rec_norm):
        if i in matched_rec:
            continue
        for j, c in enumerate(cor_norm):
            if j in matched_cor:
                continue
            # 部分一致: 片方が他方を含む、または編集距離が小さい
            if r in c or c in r or levenshtein_ratio(r, c) >= 0.7:
                partial_matches.append({"recognized": recognized_codes[i], "correct": correct_codes[j]})
                matched_rec.add(i)
                matched_cor.add(j)
                break

    misses = [correct_codes[j] for j in range(len(correct_codes)) if j not in matched_cor]
    extra = [recognized_codes[i] for i in range(len(recognized_codes)) if i not in matched_rec]

    total_correct = len(correct_codes)
    exact_rate = len(exact_matches) / total_correct if total_correct > 0 else 0.0
    partial_rate = (len(exact_matches) + len(partial_matches)) / total_correct if total_correct > 0 else 0.0

    return {
        "exact_matches": exact_matches,
        "partial_matches": partial_matches,
        "misses": misses,
        "extra": extra,
        "exact_rate": round(exact_rate, 4),
        "partial_rate": round(partial_rate, 4)
    }


def levenshtein_ratio(s1, s2):
    """編集距離ベースの類似度 (0.0〜1.0)"""
    if not s1 and not s2:
        return 1.0
    if not s1 or not s2:
        return 0.0
    max_len = max(len(s1), len(s2))
    if max_len == 0:
        return 1.0

    # 簡易的な編集距離
    m, n = len(s1), len(s2)
    dp = list(range(n + 1))
    for i in range(1, m + 1):
        prev = dp[0]
        dp[0] = i
        for j in range(1, n + 1):
            temp = dp[j]
            if s1[i-1] == s2[j-1]:
                dp[j] = prev
            else:
                dp[j] = 1 + min(prev, dp[j], dp[j-1])
            prev = temp
    dist = dp[n]
    return 1.0 - dist / max_len


def process_image(img_path, det_sess, rec_sess, char_dict):
    img = cv2.imread(str(img_path), cv2.IMREAD_COLOR)
    if img is None:
        print(f"  FAILED to load image: {img_path}")
        return None

    orig_h, orig_w = img.shape[:2]

    # Detection
    det_input, det_scale, orig_hw = det_preprocess(img)
    det_input_name = det_sess.get_inputs()[0].name
    det_output_name = det_sess.get_outputs()[0].name
    det_result = det_sess.run([det_output_name], {det_input_name: det_input})[0]
    boxes, angles, det_scores = det_postprocess(det_result, orig_hw, det_thresh=0.3)

    # Recognition with rotated crop
    result_img = img.copy()
    detections = []
    all_products = []

    for i, (box, angle, det_score) in enumerate(zip(boxes, angles, det_scores)):
        # 回転クロップ
        crop = crop_rotated(img, box, angle, padding=6)
        if crop is None:
            # フォールバック: 通常の外接矩形クロップ
            x_min, y_min = box[:, 0].min(), box[:, 1].min()
            x_max, y_max = box[:, 0].max(), box[:, 1].max()
            pad = 4
            x_min = max(0, x_min - pad)
            y_min = max(0, y_min - pad)
            x_max = min(orig_w, x_max + pad)
            y_max = min(orig_h, y_max + pad)
            if x_max <= x_min or y_max <= y_min:
                continue
            crop = img[y_min:y_max, x_min:x_max]

        text, confidence = recognize_region(crop, rec_sess, char_dict)
        text_filtered = filter_allowed(text)
        products = extract_products(text_filtered)
        all_products.extend(products)

        # 描画: 回転バウンディングボックス
        cv2.polylines(result_img, [box], True, (0, 255, 0), 2)

        # ラベル表示
        center_x = int(box[:, 0].mean())
        center_y = int(box[:, 1].mean())
        label = f"{angle:.1f}|{text_filtered}"
        cv2.putText(result_img, label, (center_x - 80, center_y - 10),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.4, (0, 0, 255), 1)

        detections.append({
            "box_index": i,
            "angle_degree": round(float(angle), 2),
            "det_score": round(float(det_score), 4),
            "box_points": box.tolist(),
            "recognized_text": text,
            "text_filtered": text_filtered,
            "confidence": round(float(confidence), 4),
            "product_codes": products
        })

    # 結果画像保存
    out_img_path = RESULTS_DIR / f"{img_path.stem}_full_pipeline.jpg"
    cv2.imwrite(str(out_img_path), result_img)

    # JSON保存
    out_json_path = RESULTS_DIR / f"{img_path.stem}_full_pipeline.json"
    result_data = {
        "image": img_path.name,
        "image_size": [orig_w, orig_h],
        "num_detections": len(detections),
        "all_product_codes": all_products,
        "detections": detections
    }
    with open(out_json_path, 'w', encoding='utf-8') as f:
        json.dump(result_data, f, indent=2, ensure_ascii=False)

    return result_data


def main():
    # 正解ラベル読み込み
    print("Loading correct labels...")
    labels = load_labels(LABEL_CSV)
    print(f"  Loaded labels for {len(labels)} images")

    # 画像ファイル収集
    img_files = sorted([f for f in IMAGES_DIR.iterdir()
                        if f.is_file() and f.suffix in IMG_EXTS])
    print(f"Found {len(img_files)} images in {IMAGES_DIR}")

    if not img_files:
        print("No images found. Exiting.")
        sys.exit(1)

    # モデル読み込み（1回だけ）
    print("Loading models...")
    det_sess = ort.InferenceSession(str(DET_MODEL))
    rec_sess = ort.InferenceSession(str(REC_MODEL))
    char_dict = load_char_dict(DICT_FILE)
    print("Models loaded.")

    # 全画像処理
    summary = []
    total_start = time.time()
    total_exact_rate = 0.0
    total_partial_rate = 0.0
    images_with_labels = 0

    for idx, img_path in enumerate(img_files, 1):
        print(f"\n[{idx}/{len(img_files)}] Processing: {img_path.name}")
        t0 = time.time()
        result = process_image(img_path, det_sess, rec_sess, char_dict)
        elapsed = time.time() - t0

        if result is None:
            summary.append({
                "image": img_path.name,
                "status": "FAILED",
                "num_detections": 0,
                "time_seconds": round(elapsed, 2)
            })
            continue

        num_det = result["num_detections"]
        all_products = result["all_product_codes"]

        # 正解ラベルとの照合
        correct_codes = labels.get(img_path.name, [])
        match_result = compute_match(all_products, correct_codes) if correct_codes else None

        if match_result:
            total_exact_rate += match_result["exact_rate"]
            total_partial_rate += match_result["partial_rate"]
            images_with_labels += 1

        print(f"  -> {num_det} regions, products: {all_products}, time: {elapsed:.2f}s")
        if match_result:
            print(f"     Exact: {match_result['exact_rate']:.0%} ({len(match_result['exact_matches'])}/{len(correct_codes)}), "
                  f"Partial: {match_result['partial_rate']:.0%}")
            if match_result["misses"]:
                print(f"     Misses: {match_result['misses']}")
            if match_result["extra"]:
                print(f"     Extra: {match_result['extra']}")

        entry = {
            "image": img_path.name,
            "status": "OK",
            "num_detections": num_det,
            "product_codes": all_products,
            "correct_codes": correct_codes,
            "time_seconds": round(elapsed, 2)
        }
        if match_result:
            entry["match"] = match_result
        summary.append(entry)

    total_elapsed = time.time() - total_start

    # 全体サマリー
    avg_exact = total_exact_rate / images_with_labels if images_with_labels > 0 else 0.0
    avg_partial = total_partial_rate / images_with_labels if images_with_labels > 0 else 0.0

    summary_data = {
        "total_images": len(img_files),
        "images_with_labels": images_with_labels,
        "total_time_seconds": round(total_elapsed, 2),
        "average_exact_match_rate": round(avg_exact, 4),
        "average_partial_match_rate": round(avg_partial, 4),
        "results": summary
    }

    summary_path = RESULTS_DIR / "_summary.json"
    with open(summary_path, 'w', encoding='utf-8') as f:
        json.dump(summary_data, f, indent=2, ensure_ascii=False)

    # コンソール出力
    print(f"\n{'='*70}")
    print(f"BATCH OCR v2 RESULTS SUMMARY")
    print(f"{'='*70}")
    print(f"Total images: {len(img_files)}")
    print(f"Images with labels: {images_with_labels}")
    print(f"Total time: {total_elapsed:.2f}s")
    print(f"Average Exact Match Rate: {avg_exact:.1%}")
    print(f"Average Partial Match Rate: {avg_partial:.1%}")
    print(f"Output directory: {RESULTS_DIR}")
    print(f"{'='*70}")

    for s in summary:
        status = s['status']
        dets = s.get('num_detections', 0)
        products = s.get('product_codes', [])
        t = s['time_seconds']
        match_str = ""
        if "match" in s:
            m = s["match"]
            match_str = f" | exact={m['exact_rate']:.0%} partial={m['partial_rate']:.0%}"
        print(f"  {s['image']:45s} | {status:6s} | {dets:3d} dets | {products}{match_str} | {t:.2f}s")
    print(f"{'='*70}")
    print(f"Summary saved to: {summary_path}")


if __name__ == "__main__":
    main()

```


## Assistant

Now let me run it:Let me fix the type issues before running:

**Tool Call: Edit OCR/scripts/batch\_full\_pipeline2.py**
Status: Completed

Diff: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/batch_full_pipeline2.py
```
import re
import sys
import csv

# === パス設定 ===
SCRIPT_DIR = Path(__file__).parent
```


**Tool Call: Edit OCR/scripts/batch\_full\_pipeline2.py**
Status: Completed

Diff: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/batch_full_pipeline2.py
```
        contour_scaled = contour.copy()
        contour_scaled[:, :, 0] = np.clip(contour_scaled[:, :, 0] / scale_x, 0, w - 1).astype(np.int32)
        contour_scaled[:, :, 1] = np.clip(contour_scaled[:, :, 1] / scale_y, 0, h - 1).astype(np.int32)
        cv2.fillPoly(mask, [contour_scaled.astype(np.int32)], 255)
        score = float(np.mean(pred[mask > 0])) if np.sum(mask) > 0 else 0.0

        box[:, 0] *= scale_x
```


**Tool Call: Edit OCR/scripts/batch\_full\_pipeline2.py**
Status: Completed

Diff: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/batch_full_pipeline2.py
```
        contour_scaled = contour.copy()
        contour_scaled[:, :, 0] = np.clip(contour_scaled[:, :, 0] / scale_x, 0, w - 1).astype(np.int32)
        contour_scaled[:, :, 1] = np.clip(contour_scaled[:, :, 1] / scale_y, 0, h - 1).astype(np.int32)
        cv2.fillPoly(mask, [contour_scaled.astype(np.int32)], (255,))
        score = float(np.mean(pred[mask > 0])) if np.sum(mask) > 0 else 0.0

        box[:, 0] *= scale_x
```


**Tool Call: cd /mnt/c/Users/soyak/StudioProjects/crossvision/OCR && python scripts/batch_full_pipeline2.py 2>&1**
Status: Failed

```
Exit code 127
/bin/bash: line 1: python: command not found
```

**Tool Call: which python3 && python3 --version**
Status: Completed

Terminal:
```
/usr/bin/python3
Python 3.12.3




```


**Tool Call: cd /mnt/c/Users/soyak/StudioProjects/crossvision/OCR && python3 scripts/batch_full_pipeline2.py 2>&1**
Status: Completed

Terminal:
```
2026-05-09 16:32:49.960548146 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.9.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960590654 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.7.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960593153 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.6.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960596209 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.2.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960598980 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.3.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960601218 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full_int_array.24.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960606981 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full_int_array.3.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960608454 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full_int_array.6.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960609999 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.4.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960614548 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.10.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960617645 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.1.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960619387 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full_int_array.0.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:32:49.960621102 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.8.0'. It is not used by any node and should be removed from the model.
Loading correct labels...
  Loaded labels for 15 images
Found 15 images in /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/images
Loading models...
Models loaded.

[1/15] Processing: 015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG
  -> 9 regions, products: ['TUA', 'UXV', 'SNOC9SIA', 'VL-NOC9SI3', 'NO9SIA', 'NO9ST'], time: 3.22s

[2/15] Processing: IMG20250416153020.jpg
  -> 7 regions, products: ['BXV', 'NOC9SIY', '119S1', 'MXA', '30-1', '31SA'], time: 2.60s
     Exact: 0% (0/2), Partial: 0%
     Misses: ['BISb30N-23', 'BISb30N-7']
     Extra: ['BXV', 'NOC9SIY', '119S1', 'MXA', '30-1', '31SA']

[3/15] Processing: IMG20250416160821.jpg
  -> 8 regions, products: ['5ST', 'BXY', 'NOSIC', '7-OE95', 'NCDO'], time: 2.65s

[4/15] Processing: IMG_0422.jpg
  -> 16 regions, products: ['30N', '31SB', '3IS630N', '30N-4', 'ISB', 'B1S530N'], time: 2.78s
     Exact: 0% (0/3), Partial: 100%
     Extra: ['31SB', '3IS630N', 'B1S530N']

[5/15] Processing: IMG_0771.jpg
  -> 5 regions, products: ['EUDYSTS', 'SHNOLGSA', '5140N2'], time: 2.63s
     Exact: 0% (0/3), Partial: 0%
     Misses: ['BISb40N-23', 'BISb40N-15', 'BISb40N-27']
     Extra: ['EUDYSTS', 'SHNOLGSA', '5140N2']

[6/15] Processing: IMG_0787.jpg
  -> 2 regions, products: ['B7S630N-11', 'AXA'], time: 2.03s
     Exact: 0% (0/1), Partial: 100%
     Extra: ['AXA']

[7/15] Processing: IMG_1536.jpg
  -> 1 regions, products: [], time: 2.15s
     Exact: 0% (0/2), Partial: 0%
     Misses: ['BISb40N-23', 'BISb40N-15']

[8/15] Processing: IMG_1537.jpg
  -> 2 regions, products: ['C-N09S'], time: 2.20s
     Exact: 0% (0/4), Partial: 0%
     Misses: ['BISb40N-27', 'BISb40N-8', 'BISb40N-26', 'BISb40N-9']
     Extra: ['C-N09S']

[9/15] Processing: IMG_1548.jpg
  -> 2 regions, products: [], time: 2.15s
     Exact: 0% (0/1), Partial: 0%
     Misses: ['BISb40N-46']

[10/15] Processing: IMG_1571.jpg
  -> 4 regions, products: ['SFNOE9SIS', 'TNOASTS'], time: 2.37s
     Exact: 0% (0/2), Partial: 0%
     Misses: ['BISb30N-13A', 'BISb30N-13']
     Extra: ['SFNOE9SIS', 'TNOASTS']

[11/15] Processing: M4sb29-2.jpg
  -> 2 regions, products: ['6-5795'], time: 1.92s
     Exact: 0% (0/1), Partial: 0%
     Misses: ['M4sb29-2']
     Extra: ['6-5795']

[12/15] Processing: M5sb24-10.jpg
  -> 3 regions, products: ['9ITA95AW', 'KOOOTAAK'], time: 2.01s
     Exact: 0% (0/1), Partial: 0%
     Misses: ['M5sb24-10']
     Extra: ['9ITA95AW', 'KOOOTAAK']

[13/15] Processing: N5G15-X1Y1.jpg
  -> 2 regions, products: ['XIX-GG', 'N7IIV'], time: 1.96s
     Exact: 0% (0/1), Partial: 0%
     Misses: ['N5G15-X1Y1']
     Extra: ['XIX-GG', 'N7IIV']

[14/15] Processing: multi_001_sunny_yellow.JPG
  -> 22 regions, products: ['IGNO9SIS', '1018', 'NO9S7', 'J-NOE9SI', '30N-34', 'IPXY', 'NCYSI2', 'BXY', '16X1', 'H-NU8', 'PSS', 'V10X', 'ID1A', 'SMYSTS', 'ALN', 'SH3ON-3'], time: 4.78s

[15/15] Processing: multi_008_yellow_green.JPG
  -> 1 regions, products: ['NOR'], time: 1.71s
     Exact: 0% (0/5), Partial: 0%
     Misses: ['BISb30N-7A', 'BISb30N-41', 'BISb30N-7A', 'BISb30N-53', 'BISb30N-37A']
     Extra: ['NOR']

======================================================================
BATCH OCR v2 RESULTS SUMMARY
======================================================================
Total images: 15
Images with labels: 12
Total time: 37.19s
Average Exact Match Rate: 0.0%
Average Partial Match Rate: 16.7%
Output directory: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/results/batch_full_pipeline2
======================================================================
  015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG | OK     |   9 dets | ['TUA', 'UXV', 'SNOC9SIA', 'VL-NOC9SI3', 'NO9SIA', 'NO9ST'] | 3.22s
  IMG20250416153020.jpg                         | OK     |   7 dets | ['BXV', 'NOC9SIY', '119S1', 'MXA', '30-1', '31SA'] | exact=0% partial=0% | 2.60s
  IMG20250416160821.jpg                         | OK     |   8 dets | ['5ST', 'BXY', 'NOSIC', '7-OE95', 'NCDO'] | 2.65s
  IMG_0422.jpg                                  | OK     |  16 dets | ['30N', '31SB', '3IS630N', '30N-4', 'ISB', 'B1S530N'] | exact=0% partial=100% | 2.78s
  IMG_0771.jpg                                  | OK     |   5 dets | ['EUDYSTS', 'SHNOLGSA', '5140N2'] | exact=0% partial=0% | 2.63s
  IMG_0787.jpg                                  | OK     |   2 dets | ['B7S630N-11', 'AXA'] | exact=0% partial=100% | 2.03s
  IMG_1536.jpg                                  | OK     |   1 dets | [] | exact=0% partial=0% | 2.15s
  IMG_1537.jpg                                  | OK     |   2 dets | ['C-N09S'] | exact=0% partial=0% | 2.20s
  IMG_1548.jpg                                  | OK     |   2 dets | [] | exact=0% partial=0% | 2.15s
  IMG_1571.jpg                                  | OK     |   4 dets | ['SFNOE9SIS', 'TNOASTS'] | exact=0% partial=0% | 2.37s
  M4sb29-2.jpg                                  | OK     |   2 dets | ['6-5795'] | exact=0% partial=0% | 1.92s
  M5sb24-10.jpg                                 | OK     |   3 dets | ['9ITA95AW', 'KOOOTAAK'] | exact=0% partial=0% | 2.01s
  N5G15-X1Y1.jpg                                | OK     |   2 dets | ['XIX-GG', 'N7IIV'] | exact=0% partial=0% | 1.96s
  multi_001_sunny_yellow.JPG                    | OK     |  22 dets | ['IGNO9SIS', '1018', 'NO9S7', 'J-NOE9SI', '30N-34', 'IPXY', 'NCYSI2', 'BXY', '16X1', 'H-NU8', 'PSS', 'V10X', 'ID1A', 'SMYSTS', 'ALN', 'SH3ON-3'] | 4.78s
  multi_008_yellow_green.JPG                    | OK     |   1 dets | ['NOR'] | exact=0% partial=0% | 1.71s
======================================================================
Summary saved to: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/results/batch_full_pipeline2/_summary.json
```


## Assistant

Script ran successfully. Now let me analyze the results. The recognition quality is poor — the model is outputting garbled text. Let me check the actual detection results and understand why:Let me look at the detailed per-image results to understand the recognition quality issue:

**Tool Call: python3 -c "
import json
from pathlib import Path

results_dir = Path('results/batch_full_pipeline2')
for json_file in sorted(results_dir.glob('*_full_pipeline.json')):
    data = json.load(open(json_file, 'r'))
    print(f'=== {data[\"image\"]} ({data[\"num_detections\"]} dets) ===')
    for d in data['detections']:
        print(f'  #{d[\"box_index\"]} angle={d[\"angle_degree\"]:.1f} conf={d[\"confidence\"]:.3f} text=\"{d[\"recognized_text\"]}\" filtered=\"{d[\"text_filtered\"]}\"')
    print()
" 2>&1**
Status: Completed

Terminal:
```
=== 015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG (9 dets) ===
  #0 angle=40.9 conf=0.014 text="TUA" filtered="TUA"
  #1 angle=45.0 conf=0.014 text="OX" filtered="OX"
  #2 angle=55.0 conf=0.015 text="UXV" filtered="UXV"
  #3 angle=45.7 conf=0.013 text="S?Noc?9SIa" filtered="SNoc9SIa"
  #4 angle=56.3 conf=0.012 text="0" filtered="0"
  #5 angle=56.4 conf=0.017 text="VL-NOC9SI3" filtered="VL-NOC9SI3"
  #6 angle=63.8 conf=0.015 text="NO9SIA" filtered="NO9SIA"
  #7 angle=59.5 conf=0.017 text="-No9sT" filtered="-No9sT"
  #8 angle=53.1 conf=0.000 text="" filtered=""

=== IMG20250416153020.jpg (7 dets) ===
  #0 angle=10.2 conf=0.019 text="bXV" filtered="bXV"
  #1 angle=9.5 conf=0.018 text="-NOC9SIY" filtered="-NOC9SIY"
  #2 angle=4.5 conf=0.013 text="119S1" filtered="119S1"
  #3 angle=90.0 conf=0.019 text="M?Xa" filtered="MXa"
  #4 angle=87.4 conf=0.020 text="30-1" filtered="30-1"
  #5 angle=85.4 conf=0.019 text="XO" filtered="XO"
  #6 angle=87.5 conf=0.018 text="31SA" filtered="31SA"

=== IMG20250416160821.jpg (8 dets) ===
  #0 angle=90.0 conf=0.011 text="S" filtered="S"
  #1 angle=90.0 conf=0.013 text="G" filtered="G"
  #2 angle=90.0 conf=0.015 text="5St" filtered="5St"
  #3 angle=5.7 conf=0.024 text="bXY" filtered="bXY"
  #4 angle=4.1 conf=0.017 text="-NOSIC" filtered="-NOSIC"
  #5 angle=81.6 conf=0.013 text="Ry" filtered="Ry"
  #6 angle=86.4 conf=0.014 text="7-OE95" filtered="7-OE95"
  #7 angle=7.1 conf=0.017 text="nc?do" filtered="ncdo"

=== IMG_0422.jpg (16 dets) ===
  #0 angle=4.1 conf=0.021 text="30N" filtered="30N"
  #1 angle=1.6 conf=0.019 text="31Sb" filtered="31Sb"
  #2 angle=3.1 conf=0.015 text="XA" filtered="XA"
  #3 angle=90.0 conf=0.020 text="bX" filtered="bX"
  #4 angle=7.1 conf=0.015 text="C" filtered="C"
  #5 angle=8.1 conf=0.021 text="A" filtered="A"
  #6 angle=8.4 conf=0.023 text="3IS630N" filtered="3IS630N"
  #7 angle=7.1 conf=0.014 text="Xa" filtered="Xa"
  #8 angle=7.3 conf=0.022 text="Xa" filtered="Xa"
  #9 angle=15.9 conf=0.000 text="" filtered=""
  #10 angle=10.0 conf=0.023 text="30N-4" filtered="30N-4"
  #11 angle=9.5 conf=0.018 text="ISb" filtered="ISb"
  #12 angle=14.0 conf=0.013 text="C" filtered="C"
  #13 angle=14.0 conf=0.000 text="" filtered=""
  #14 angle=14.0 conf=0.000 text="" filtered=""
  #15 angle=11.8 conf=0.018 text="B1S530N" filtered="B1S530N"

=== IMG_0771.jpg (5 dets) ===
  #0 angle=74.0 conf=0.016 text="LX" filtered="LX"
  #1 angle=77.0 conf=0.015 text="EUDYSTS" filtered="EUDYSTS"
  #2 angle=84.4 conf=0.013 text="a" filtered="a"
  #3 angle=85.7 conf=0.015 text="SHNOLGSa" filtered="SHNOLGSa"
  #4 angle=8.2 conf=0.019 text="5140N2" filtered="5140N2"

=== IMG_0787.jpg (2 dets) ===
  #0 angle=2.2 conf=0.020 text="B7S630N-11" filtered="B7S630N-11"
  #1 angle=1.6 conf=0.014 text="AXa" filtered="AXa"

=== IMG_1536.jpg (1 dets) ===
  #0 angle=7.1 conf=0.000 text="" filtered=""

=== IMG_1537.jpg (2 dets) ===
  #0 angle=78.6 conf=0.012 text="C-N09S" filtered="C-N09S"
  #1 angle=87.3 conf=0.007 text="N?" filtered="N"

=== IMG_1548.jpg (2 dets) ===
  #0 angle=0.0 conf=0.000 text="" filtered=""
  #1 angle=1.8 conf=0.012 text="W" filtered="W"

=== IMG_1571.jpg (4 dets) ===
  #0 angle=90.0 conf=0.000 text="" filtered=""
  #1 angle=83.7 conf=0.011 text="6" filtered="6"
  #2 angle=1.3 conf=0.016 text="SFNOE9SIS" filtered="SFNOE9SIS"
  #3 angle=82.3 conf=0.021 text="TNOASTS" filtered="TNOASTS"

=== M4sb29-2.jpg (2 dets) ===
  #0 angle=89.1 conf=0.019 text="6-5795+w" filtered="6-5795+w"
  #1 angle=90.0 conf=0.000 text="" filtered=""

=== M5sb24-10.jpg (3 dets) ===
  #0 angle=88.7 conf=0.014 text="9ita95aw" filtered="9ita95aw"
  #1 angle=90.0 conf=0.000 text="" filtered=""
  #2 angle=89.0 conf=0.013 text="KOOOTAAK" filtered="KOOOTAAK"

=== N5G15-X1Y1.jpg (2 dets) ===
  #0 angle=83.8 conf=0.022 text="XIX-GG" filtered="XIX-GG"
  #1 angle=86.0 conf=0.017 text="N7IIV" filtered="N7IIV"

=== multi_001_sunny_yellow.JPG (22 dets) ===
  #0 angle=1.4 conf=0.000 text="" filtered=""
  #1 angle=1.3 conf=0.018 text="IGNO9SIS" filtered="IGNO9SIS"
  #2 angle=90.0 conf=0.013 text="S" filtered="S"
  #3 angle=90.0 conf=0.016 text="1018" filtered="1018"
  #4 angle=90.0 conf=0.000 text="" filtered=""
  #5 angle=1.6 conf=0.018 text="NO9S7" filtered="NO9S7"
  #6 angle=90.0 conf=0.016 text="J-NOE9SI" filtered="J-NOE9SI"
  #7 angle=1.2 conf=0.022 text="30N-34" filtered="30N-34"
  #8 angle=88.2 conf=0.014 text="S2" filtered="S2"
  #9 angle=86.2 conf=0.013 text="I?PXY" filtered="IPXY"
  #10 angle=90.0 conf=0.015 text="bY" filtered="bY"
  #11 angle=89.7 conf=0.014 text="-NCYSI2" filtered="-NCYSI2"
  #12 angle=88.6 conf=0.019 text="bXY" filtered="bXY"
  #13 angle=1.0 conf=0.019 text="3/Sh" filtered="3/Sh"
  #14 angle=90.0 conf=0.012 text="16X?1" filtered="16X1"
  #15 angle=90.0 conf=0.022 text="H-NU8" filtered="H-NU8"
  #16 angle=90.0 conf=0.012 text="Pss" filtered="Pss"
  #17 angle=3.8 conf=0.014 text="V10X" filtered="V10X"
  #18 angle=90.0 conf=0.016 text="ID1A" filtered="ID1A"
  #19 angle=1.5 conf=0.015 text="SMySTs" filtered="SMySTs"
  #20 angle=90.0 conf=0.024 text="aLN" filtered="aLN"
  #21 angle=90.0 conf=0.023 text="/Sh3oN-3" filtered="/Sh3oN-3"

=== multi_008_yellow_green.JPG (1 dets) ===
  #0 angle=14.0 conf=0.015 text="Nor." filtered="Nor."
```


## Assistant

The results show a clear pattern: the recognition confidence is extremely low (0.01-0.02 range), and the model is producing garbled text. This is a fundamental model accuracy issue — the `en_PP-OCRv3_rec.onnx` model is an English-oriented model that doesn't handle the Japanese industrial label fonts well.

Let me check the summary JSON and also look at the visualized results:

**Tool Call: Read OCR/results/batch_full_pipeline2/_summary.json**
Status: Completed

```
1	{
2	  "total_images": 15,
3	  "images_with_labels": 12,
4	  "total_time_seconds": 37.19,
5	  "average_exact_match_rate": 0.0,
6	  "average_partial_match_rate": 0.1667,
7	  "results": [
8	    {
9	      "image": "015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG",
10	      "status": "OK",
11	      "num_detections": 9,
12	      "product_codes": [
13	        "TUA",
14	        "UXV",
15	        "SNOC9SIA",
16	        "VL-NOC9SI3",
17	        "NO9SIA",
18	        "NO9ST"
19	      ],
20	      "correct_codes": [],
21	      "time_seconds": 3.22
22	    },
23	    {
24	      "image": "IMG20250416153020.jpg",
25	      "status": "OK",
26	      "num_detections": 7,
27	      "product_codes": [
28	        "BXV",
29	        "NOC9SIY",
30	        "119S1",
31	        "MXA",
32	        "30-1",
33	        "31SA"
34	      ],
35	      "correct_codes": [
36	        "BISb30N-23",
37	        "BISb30N-7"
38	      ],
39	      "time_seconds": 2.6,
40	      "match": {
41	        "exact_matches": [],
42	        "partial_matches": [],
43	        "misses": [
44	          "BISb30N-23",
45	          "BISb30N-7"
46	        ],
47	        "extra": [
48	          "BXV",
49	          "NOC9SIY",
50	          "119S1",
51	          "MXA",
52	          "30-1",
53	          "31SA"
54	        ],
55	        "exact_rate": 0.0,
56	        "partial_rate": 0.0
57	      }
58	    },
59	    {
60	      "image": "IMG20250416160821.jpg",
61	      "status": "OK",
62	      "num_detections": 8,
63	      "product_codes": [
64	        "5ST",
65	        "BXY",
66	        "NOSIC",
67	        "7-OE95",
68	        "NCDO"
69	      ],
70	      "correct_codes": [],
71	      "time_seconds": 2.65
72	    },
73	    {
74	      "image": "IMG_0422.jpg",
75	      "status": "OK",
76	      "num_detections": 16,
77	      "product_codes": [
78	        "30N",
79	        "31SB",
80	        "3IS630N",
81	        "30N-4",
82	        "ISB",
83	        "B1S530N"
84	      ],
85	      "correct_codes": [
86	        "BISb30N-7A",
87	        "BISb30N-4I",
88	        "BISb30N-7A"
89	      ],
90	      "time_seconds": 2.78,
91	      "match": {
92	        "exact_matches": [],
93	        "partial_matches": [
94	          {
95	            "recognized": "30N",
96	            "correct": "BISb30N-7A"
97	          },
98	          {
99	            "recognized": "30N-4",
100	            "correct": "BISb30N-4I"
101	          },
102	          {
103	            "recognized": "ISB",
104	            "correct": "BISb30N-7A"
105	          }
106	        ],
107	        "misses": [],
108	        "extra": [
109	          "31SB",
110	          "3IS630N",
111	          "B1S530N"
112	        ],
113	        "exact_rate": 0.0,
114	        "partial_rate": 1.0
115	      }
116	    },
117	    {
118	      "image": "IMG_0771.jpg",
119	      "status": "OK",
120	      "num_detections": 5,
121	      "product_codes": [
122	        "EUDYSTS",
123	        "SHNOLGSA",
124	        "5140N2"
125	      ],
126	      "correct_codes": [
127	        "BISb40N-23",
128	        "BISb40N-15",
129	        "BISb40N-27"
130	      ],
131	      "time_seconds": 2.63,
132	      "match": {
133	        "exact_matches": [],
134	        "partial_matches": [],
135	        "misses": [
136	          "BISb40N-23",
137	          "BISb40N-15",
138	          "BISb40N-27"
139	        ],
140	        "extra": [
141	          "EUDYSTS",
142	          "SHNOLGSA",
143	          "5140N2"
144	        ],
145	        "exact_rate": 0.0,
146	        "partial_rate": 0.0
147	      }
148	    },
149	    {
150	      "image": "IMG_0787.jpg",
151	      "status": "OK",
152	      "num_detections": 2,
153	      "product_codes": [
154	        "B7S630N-11",
155	        "AXA"
156	      ],
157	      "correct_codes": [
158	        "BISb30N-16"
159	      ],
160	      "time_seconds": 2.03,
161	      "match": {
162	        "exact_matches": [],
163	        "partial_matches": [
164	          {
165	            "recognized": "B7S630N-11",
166	            "correct": "BISb30N-16"
167	          }
168	        ],
169	        "misses": [],
170	        "extra": [
171	          "AXA"
172	        ],
173	        "exact_rate": 0.0,
174	        "partial_rate": 1.0
175	      }
176	    },
177	    {
178	      "image": "IMG_1536.jpg",
179	      "status": "OK",
180	      "num_detections": 1,
181	      "product_codes": [],
182	      "correct_codes": [
183	        "BISb40N-23",
184	        "BISb40N-15"
185	      ],
186	      "time_seconds": 2.15,
187	      "match": {
188	        "exact_matches": [],
189	        "partial_matches": [],
190	        "misses": [
191	          "BISb40N-23",
192	          "BISb40N-15"
193	        ],
194	        "extra": [],
195	        "exact_rate": 0.0,
196	        "partial_rate": 0.0
197	      }
198	    },
199	    {
200	      "image": "IMG_1537.jpg",
201	      "status": "OK",
202	      "num_detections": 2,
203	      "product_codes": [
204	        "C-N09S"
205	      ],
206	      "correct_codes": [
207	        "BISb40N-27",
208	        "BISb40N-8",
209	        "BISb40N-26",
210	        "BISb40N-9"
211	      ],
212	      "time_seconds": 2.2,
213	      "match": {
214	        "exact_matches": [],
215	        "partial_matches": [],
216	        "misses": [
217	          "BISb40N-27",
218	          "BISb40N-8",
219	          "BISb40N-26",
220	          "BISb40N-9"
221	        ],
222	        "extra": [
223	          "C-N09S"
224	        ],
225	        "exact_rate": 0.0,
226	        "partial_rate": 0.0
227	      }
228	    },
229	    {
230	      "image": "IMG_1548.jpg",
231	      "status": "OK",
232	      "num_detections": 2,
233	      "product_codes": [],
234	      "correct_codes": [
235	        "BISb40N-46"
236	      ],
237	      "time_seconds": 2.15,
238	      "match": {
239	        "exact_matches": [],
240	        "partial_matches": [],
241	        "misses": [
242	          "BISb40N-46"
243	        ],
244	        "extra": [],
245	        "exact_rate": 0.0,
246	        "partial_rate": 0.0
247	      }
248	    },
249	    {
250	      "image": "IMG_1571.jpg",
251	      "status": "OK",
252	      "num_detections": 4,
253	      "product_codes": [
254	        "SFNOE9SIS",
255	        "TNOASTS"
256	      ],
257	      "correct_codes": [
258	        "BISb30N-13A",
259	        "BISb30N-13"
260	      ],
261	      "time_seconds": 2.37,
262	      "match": {
263	        "exact_matches": [],
264	        "partial_matches": [],
265	        "misses": [
266	          "BISb30N-13A",
267	          "BISb30N-13"
268	        ],
269	        "extra": [
270	          "SFNOE9SIS",
271	          "TNOASTS"
272	        ],
273	        "exact_rate": 0.0,
274	        "partial_rate": 0.0
275	      }
276	    },
277	    {
278	      "image": "M4sb29-2.jpg",
279	      "status": "OK",
280	      "num_detections": 2,
281	      "product_codes": [
282	        "6-5795"
283	      ],
284	      "correct_codes": [
285	        "M4sb29-2"
286	      ],
287	      "time_seconds": 1.92,
288	      "match": {
289	        "exact_matches": [],
290	        "partial_matches": [],
291	        "misses": [
292	          "M4sb29-2"
293	        ],
294	        "extra": [
295	          "6-5795"
296	        ],
297	        "exact_rate": 0.0,
298	        "partial_rate": 0.0
299	      }
300	    },
301	    {
302	      "image": "M5sb24-10.jpg",
303	      "status": "OK",
304	      "num_detections": 3,
305	      "product_codes": [
306	        "9ITA95AW",
307	        "KOOOTAAK"
308	      ],
309	      "correct_codes": [
310	        "M5sb24-10"
311	      ],
312	      "time_seconds": 2.01,
313	      "match": {
314	        "exact_matches": [],
315	        "partial_matches": [],
316	        "misses": [
317	          "M5sb24-10"
318	        ],
319	        "extra": [
320	          "9ITA95AW",
321	          "KOOOTAAK"
322	        ],
323	        "exact_rate": 0.0,
324	        "partial_rate": 0.0
325	      }
326	    },
327	    {
328	      "image": "N5G15-X1Y1.jpg",
329	      "status": "OK",
330	      "num_detections": 2,
331	      "product_codes": [
332	        "XIX-GG",
333	        "N7IIV"
334	      ],
335	      "correct_codes": [
336	        "N5G15-X1Y1"
337	      ],
338	      "time_seconds": 1.96,
339	      "match": {
340	        "exact_matches": [],
341	        "partial_matches": [],
342	        "misses": [
343	          "N5G15-X1Y1"
344	        ],
345	        "extra": [
346	          "XIX-GG",
347	          "N7IIV"
348	        ],
349	        "exact_rate": 0.0,
350	        "partial_rate": 0.0
351	      }
352	    },
353	    {
354	      "image": "multi_001_sunny_yellow.JPG",
355	      "status": "OK",
356	      "num_detections": 22,
357	      "product_codes": [
358	        "IGNO9SIS",
359	        "1018",
360	        "NO9S7",
361	        "J-NOE9SI",
362	        "30N-34",
363	        "IPXY",
364	        "NCYSI2",
365	        "BXY",
366	        "16X1",
367	        "H-NU8",
368	        "PSS",
369	        "V10X",
370	        "ID1A",
371	        "SMYSTS",
372	        "ALN",
373	        "SH3ON-3"
374	      ],
375	      "correct_codes": [],
376	      "time_seconds": 4.78
377	    },
378	    {
379	      "image": "multi_008_yellow_green.JPG",
380	      "status": "OK",
381	      "num_detections": 1,
382	      "product_codes": [
383	        "NOR"
384	      ],
385	      "correct_codes": [
386	        "BISb30N-7A",
387	        "BISb30N-41",
388	        "BISb30N-7A",
389	        "BISb30N-53",
390	        "BISb30N-37A"
391	      ],
392	      "time_seconds": 1.71,
393	      "match": {
394	        "exact_matches": [],
395	        "partial_matches": [],
396	        "misses": [
397	          "BISb30N-7A",
398	          "BISb30N-41",
399	          "BISb30N-7A",
400	          "BISb30N-53",
401	          "BISb30N-37A"
402	        ],
403	        "extra": [
404	          "NOR"
405	        ],
406	        "exact_rate": 0.0,
407	        "partial_rate": 0.0
408	      }
409	    }
410	  ]
411	}
```

## Assistant

Now let me also check the label CSV parsing — I notice some images with labels in the CSV don't seem to have matched. Let me verify:

**Tool Call: python3 -c "
import csv

with open('images/正解ラベル.csv', 'r', encoding='utf-8') as f:
    reader = csv.reader(f)
    header = next(reader)
    print('Header:', header)
    print()
    for row in reader:
        if len(row) >= 8:
            fname = row[1].strip()
            codes = row[7].strip()
            code_list = [c.strip() for c in codes.split('\n') if c.strip()]
            print(f'{fname:50s} -> {code_list}')
" 2>&1**
Status: Completed

Terminal:
```
Header: ['列1', 'Filename', '色', 'Noise', '傾き', '光線', '文字列個数', '正解コード']

IMG_0787.jpg                                       -> ['BISb30N-16']
M5sb24-10.jpg                                      -> ['M5sb24-10']
IMG_1537.jpg                                       -> ['BISb40N-27', 'BISb40N-8', 'BISb40N-26', 'BISb40N-9']
IMG_1548.jpg                                       -> ['BISb40N-46']
IMG_1571.jpg                                       -> ['BISb30N-13A', 'BISb30N-13']
IMG_0422.jpg                                       -> ['BISb30N-7A', 'BISb30N-4I', 'BISb30N-7A']
IMG20250416153020.jpg                              -> ['BISb30N-23', 'BISb30N-7']
multi_008_yellow_green.JPG                         -> ['BISb30N-7A', 'BISb30N-41', 'BISb30N-7A', 'BISb30N-53', 'BISb30N-37A']
N5G15-X1Y1.jpg                                     -> ['N5G15-X1Y1']
bundle/multi_001_sunny_yellow.JPG                  -> ['BISb30N-36', 'BISb30N-40', 'BISb30N-19', 'BISb30N-20', 'BISb30N-56', 'BISb30N-34', 'BISb30N-12', 'BISb30N-26']
b015_B15b30N-41__B15b30N-7A_45deg_NW_yellow        -> ['BISb30N-7A', 'BISb30N-4I', 'BISb30N-7A', 'BISb30N-53']
IIMG20250416160821.jpg                             -> ['BISb30N-19', 'BISb30N-20']
IMG_1536.jpg                                       -> ['BISb40N-23', 'BISb40N-15']
IMG_0771.jpg                                       -> ['BISb40N-23', 'BISb40N-15', 'BISb40N-27']
M4sb29-2.jpg                                       -> ['M4sb29-2']
```


## Assistant

I see the issue — some filenames in the CSV don't match the actual image filenames. For example:
- `bundle/multi_001_sunny_yellow.JPG` in CSV vs `multi_001_sunny_yellow.JPG` in images
- `b015_B15b30N-41__B15b30N-7A_45deg_NW_yellow` (no extension) in CSV vs `015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG`
- `IIMG20250416160821.jpg` in CSV vs `IMG20250416160821.jpg` in images

Let me fix the label loading to handle these filename mismatches:

**Tool Call: Read OCR/scripts/batch_full_pipeline2.py (55 - 84)**
Status: Completed

```
55	    return chars
56	
57	
58	def load_labels(csv_path):
59	    """正解ラベルCSVを読み込む。ファイル名→正解コードリストの辞書を返す"""
60	    labels = {}
61	    with open(csv_path, 'r', encoding='utf-8') as f:
62	        reader = csv.reader(f)
63	        header = next(reader)  # skip header
64	        for row in reader:
65	            if len(row) < 8:
66	                continue
67	            filename_raw = row[1].strip()
68	            # ファイル名の末尾に.jpg等が含まれている場合と含まれていない場合がある
69	            # 改行が入っている場合もあるので除去
70	            filename = filename_raw.strip().replace('\n', '').replace('\r', '')
71	            # 拡張子がない場合は .jpg を補完
72	            if not Path(filename).suffix:
73	                filename += ".jpg"
74	            correct_codes = row[7].strip()
75	            # 正解コードは改行区切りで複数ある場合がある
76	            codes = [c.strip() for c in correct_codes.split('\n') if c.strip()]
77	            labels[filename] = codes
78	    return labels
79	
80	
81	def det_preprocess(img):
82	    h, w = img.shape[:2]
83	    scale = min(960 / h, 960 / w)
84	    nh = int(h * scale) // 32 * 32
```

**Tool Call: Edit OCR/scripts/batch\_full\_pipeline2.py**
Status: Completed

Diff: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/batch_full_pipeline2.py
```
    return chars


def normalize_filename_for_matching(name):
    """
    CSVのファイル名と画像ファイル名の不一致を吸収する。
    - ディレクトリプレフィックス (bundle/ 等) を除去
    - 先頭の余分な文字 (b015_ → 015_, II → I) を修正
    - 拡張子がない場合は .jpg を補完
    - 大文字小文字を統一
    """
    name = name.strip().replace('\n', '').replace('\r', '')
    # ディレクトリプレフィックス除去
    name = Path(name).name
    # 拡張子がない場合は .jpg 補完
    if not Path(name).suffix:
        name += ".jpg"
    # 先頭の余分な文字を修正
    # b015_ → 015_
    if name.startswith('b') and len(name) > 1 and name[1:5].isdigit():
        name = name[1:]
    # II → I (IIMG → IMG)
    if name.startswith('II') and not name.startswith('III'):
        name = name[1:]
    return name.lower()


def load_labels(csv_path, img_files):
    """正解ラベルCSVを読み込む。画像ファイル名→正解コードリストの辞書を返す"""
    # 画像ファイル名の正規化マップ
    img_names = {f.name.lower(): f.name for f in img_files}

    labels = {}
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
```


Diff: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/batch_full_pipeline2.py
```
            if len(row) < 8:
                continue
            filename_raw = row[1].strip()
            correct_codes = row[7].strip()
            codes = [c.strip() for c in correct_codes.split('\n') if c.strip()]
            if not codes:
                continue

            norm = normalize_filename_for_matching(filename_raw)
            # 完全一致
            if norm in img_names:
                labels[img_names[norm]] = codes
            else:
                # 部分一致: stem でマッチ
                stem = Path(norm).stem
                for lower_name, orig_name in img_names.items():
                    if Path(lower_name).stem == stem:
                        labels[orig_name] = codes
                        break
                else:
                    print(f"  WARNING: No matching image for CSV label '{filename_raw}' (normalized: '{norm}')")
    return labels


```


## Assistant

Now update the `main()` to pass `img_files` to `load_labels`:

**Tool Call: Edit OCR/scripts/batch\_full\_pipeline2.py**
Status: Completed

Diff: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/batch_full_pipeline2.py
```


def main():
    # 画像ファイル収集
    img_files = sorted([f for f in IMAGES_DIR.iterdir()
                        if f.is_file() and f.suffix in IMG_EXTS])
    print(f"Found {len(img_files)} images in {IMAGES_DIR}")

    # 正解ラベル読み込み
    print("Loading correct labels...")
    labels = load_labels(LABEL_CSV, img_files)
    print(f"  Loaded labels for {len(labels)} images")

    # 画像ファイル収集
```


## Assistant

Now remove the duplicate `img_files` collection:

**Tool Call: Read OCR/scripts/batch_full_pipeline2.py (380 - 409)**
Status: Completed

```
380	        dp[0] = i
381	        for j in range(1, n + 1):
382	            temp = dp[j]
383	            if s1[i-1] == s2[j-1]:
384	                dp[j] = prev
385	            else:
386	                dp[j] = 1 + min(prev, dp[j], dp[j-1])
387	            prev = temp
388	    dist = dp[n]
389	    return 1.0 - dist / max_len
390	
391	
392	def process_image(img_path, det_sess, rec_sess, char_dict):
393	    img = cv2.imread(str(img_path), cv2.IMREAD_COLOR)
394	    if img is None:
395	        print(f"  FAILED to load image: {img_path}")
396	        return None
397	
398	    orig_h, orig_w = img.shape[:2]
399	
400	    # Detection
401	    det_input, det_scale, orig_hw = det_preprocess(img)
402	    det_input_name = det_sess.get_inputs()[0].name
403	    det_output_name = det_sess.get_outputs()[0].name
404	    det_result = det_sess.run([det_output_name], {det_input_name: det_input})[0]
405	    boxes, angles, det_scores = det_postprocess(det_result, orig_hw, det_thresh=0.3)
406	
407	    # Recognition with rotated crop
408	    result_img = img.copy()
409	    detections = []
```

**Tool Call: Read OCR/scripts/batch_full_pipeline2.py (440 - 469)**
Status: Completed

```
440	        cv2.putText(result_img, label, (center_x - 80, center_y - 10),
441	                   cv2.FONT_HERSHEY_SIMPLEX, 0.4, (0, 0, 255), 1)
442	
443	        detections.append({
444	            "box_index": i,
445	            "angle_degree": round(float(angle), 2),
446	            "det_score": round(float(det_score), 4),
447	            "box_points": box.tolist(),
448	            "recognized_text": text,
449	            "text_filtered": text_filtered,
450	            "confidence": round(float(confidence), 4),
451	            "product_codes": products
452	        })
453	
454	    # 結果画像保存
455	    out_img_path = RESULTS_DIR / f"{img_path.stem}_full_pipeline.jpg"
456	    cv2.imwrite(str(out_img_path), result_img)
457	
458	    # JSON保存
459	    out_json_path = RESULTS_DIR / f"{img_path.stem}_full_pipeline.json"
460	    result_data = {
461	        "image": img_path.name,
462	        "image_size": [orig_w, orig_h],
463	        "num_detections": len(detections),
464	        "all_product_codes": all_products,
465	        "detections": detections
466	    }
467	    with open(out_json_path, 'w', encoding='utf-8') as f:
468	        json.dump(result_data, f, indent=2, ensure_ascii=False)
469	
```

**Tool Call: Read OCR/scripts/batch_full_pipeline2.py (470 - 499)**
Status: Completed

```
470	    return result_data
471	
472	
473	def main():
474	    # 画像ファイル収集
475	    img_files = sorted([f for f in IMAGES_DIR.iterdir()
476	                        if f.is_file() and f.suffix in IMG_EXTS])
477	    print(f"Found {len(img_files)} images in {IMAGES_DIR}")
478	
479	    # 正解ラベル読み込み
480	    print("Loading correct labels...")
481	    labels = load_labels(LABEL_CSV, img_files)
482	    print(f"  Loaded labels for {len(labels)} images")
483	
484	    # 画像ファイル収集
485	    img_files = sorted([f for f in IMAGES_DIR.iterdir()
486	                        if f.is_file() and f.suffix in IMG_EXTS])
487	    print(f"Found {len(img_files)} images in {IMAGES_DIR}")
488	
489	    if not img_files:
490	        print("No images found. Exiting.")
491	        sys.exit(1)
492	
493	    # モデル読み込み（1回だけ）
494	    print("Loading models...")
495	    det_sess = ort.InferenceSession(str(DET_MODEL))
496	    rec_sess = ort.InferenceSession(str(REC_MODEL))
497	    char_dict = load_char_dict(DICT_FILE)
498	    print("Models loaded.")
499	
```

**Tool Call: Edit OCR/scripts/batch\_full\_pipeline2.py**
Status: Completed

Diff: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/batch_full_pipeline2.py
```
    labels = load_labels(LABEL_CSV, img_files)
    print(f"  Loaded labels for {len(labels)} images")

    if not img_files:
        print("No images found. Exiting.")
        sys.exit(1)
```


## Assistant

Now re-run:

**Tool Call: python3 scripts/batch_full_pipeline2.py 2>&1**
Status: Completed

Terminal:
```
2026-05-09 16:41:25.695483880 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.9.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695546889 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.7.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695550490 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.6.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695554721 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.2.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695559595 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.3.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695563211 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full_int_array.24.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695570947 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full_int_array.3.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695573256 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full_int_array.6.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695575688 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.4.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695582848 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.10.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695587459 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.1.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695589984 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full_int_array.0.0'. It is not used by any node and should be removed from the model.
2026-05-09 16:41:25.695592567 [W:onnxruntime:, graph.cc:5307 CleanUnusedInitializersAndNodeArgs] Removing initializer 'p2o.pd_op.full.8.0'. It is not used by any node and should be removed from the model.
Found 15 images in /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/images
Loading correct labels...
  WARNING: No matching image for CSV label 'b015_B15b30N-41__B15b30N-7A_45deg_NW_yellow' (normalized: 'b015_b15b30n-41__b15b30n-7a_45deg_nw_yellow.jpg')
  Loaded labels for 14 images
Loading models...
Models loaded.

[1/15] Processing: 015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG
  -> 9 regions, products: ['TUA', 'UXV', 'SNOC9SIA', 'VL-NOC9SI3', 'NO9SIA', 'NO9ST'], time: 4.11s

[2/15] Processing: IMG20250416153020.jpg
  -> 7 regions, products: ['BXV', 'NOC9SIY', '119S1', 'MXA', '30-1', '31SA'], time: 3.14s
     Exact: 0% (0/2), Partial: 0%
     Misses: ['BISb30N-23', 'BISb30N-7']
     Extra: ['BXV', 'NOC9SIY', '119S1', 'MXA', '30-1', '31SA']

[3/15] Processing: IMG20250416160821.jpg
  -> 8 regions, products: ['5ST', 'BXY', 'NOSIC', '7-OE95', 'NCDO'], time: 3.04s
     Exact: 0% (0/2), Partial: 0%
     Misses: ['BISb30N-19', 'BISb30N-20']
     Extra: ['5ST', 'BXY', 'NOSIC', '7-OE95', 'NCDO']

[4/15] Processing: IMG_0422.jpg
  -> 16 regions, products: ['30N', '31SB', '3IS630N', '30N-4', 'ISB', 'B1S530N'], time: 2.98s
     Exact: 0% (0/3), Partial: 100%
     Extra: ['31SB', '3IS630N', 'B1S530N']

[5/15] Processing: IMG_0771.jpg
  -> 5 regions, products: ['EUDYSTS', 'SHNOLGSA', '5140N2'], time: 2.69s
     Exact: 0% (0/3), Partial: 0%
     Misses: ['BISb40N-23', 'BISb40N-15', 'BISb40N-27']
     Extra: ['EUDYSTS', 'SHNOLGSA', '5140N2']

[6/15] Processing: IMG_0787.jpg
  -> 2 regions, products: ['B7S630N-11', 'AXA'], time: 2.40s
     Exact: 0% (0/1), Partial: 100%
     Extra: ['AXA']

[7/15] Processing: IMG_1536.jpg
  -> 1 regions, products: [], time: 2.39s
     Exact: 0% (0/2), Partial: 0%
     Misses: ['BISb40N-23', 'BISb40N-15']

[8/15] Processing: IMG_1537.jpg
  -> 2 regions, products: ['C-N09S'], time: 2.44s
     Exact: 0% (0/4), Partial: 0%
     Misses: ['BISb40N-27', 'BISb40N-8', 'BISb40N-26', 'BISb40N-9']
     Extra: ['C-N09S']

[9/15] Processing: IMG_1548.jpg
  -> 2 regions, products: [], time: 1.33s
     Exact: 0% (0/1), Partial: 0%
     Misses: ['BISb40N-46']

[10/15] Processing: IMG_1571.jpg
  -> 4 regions, products: ['SFNOE9SIS', 'TNOASTS'], time: 2.89s
     Exact: 0% (0/2), Partial: 0%
     Misses: ['BISb30N-13A', 'BISb30N-13']
     Extra: ['SFNOE9SIS', 'TNOASTS']

[11/15] Processing: M4sb29-2.jpg
  -> 2 regions, products: ['6-5795'], time: 1.97s
     Exact: 0% (0/1), Partial: 0%
     Misses: ['M4sb29-2']
     Extra: ['6-5795']

[12/15] Processing: M5sb24-10.jpg
  -> 3 regions, products: ['9ITA95AW', 'KOOOTAAK'], time: 2.35s
     Exact: 0% (0/1), Partial: 0%
     Misses: ['M5sb24-10']
     Extra: ['9ITA95AW', 'KOOOTAAK']

[13/15] Processing: N5G15-X1Y1.jpg
  -> 2 regions, products: ['XIX-GG', 'N7IIV'], time: 2.10s
     Exact: 0% (0/1), Partial: 0%
     Misses: ['N5G15-X1Y1']
     Extra: ['XIX-GG', 'N7IIV']

[14/15] Processing: multi_001_sunny_yellow.JPG
  -> 22 regions, products: ['IGNO9SIS', '1018', 'NO9S7', 'J-NOE9SI', '30N-34', 'IPXY', 'NCYSI2', 'BXY', '16X1', 'H-NU8', 'PSS', 'V10X', 'ID1A', 'SMYSTS', 'ALN', 'SH3ON-3'], time: 5.37s
     Exact: 0% (0/8), Partial: 12%
     Misses: ['BISb30N-36', 'BISb30N-40', 'BISb30N-19', 'BISb30N-20', 'BISb30N-56', 'BISb30N-12', 'BISb30N-26']
     Extra: ['IGNO9SIS', '1018', 'NO9S7', 'J-NOE9SI', 'IPXY', 'NCYSI2', 'BXY', '16X1', 'H-NU8', 'PSS', 'V10X', 'ID1A', 'SMYSTS', 'ALN', 'SH3ON-3']

[15/15] Processing: multi_008_yellow_green.JPG
  -> 1 regions, products: ['NOR'], time: 2.80s
     Exact: 0% (0/5), Partial: 0%
     Misses: ['BISb30N-7A', 'BISb30N-41', 'BISb30N-7A', 'BISb30N-53', 'BISb30N-37A']
     Extra: ['NOR']

======================================================================
BATCH OCR v2 RESULTS SUMMARY
======================================================================
Total images: 15
Images with labels: 14
Total time: 42.00s
Average Exact Match Rate: 0.0%
Average Partial Match Rate: 15.2%
Output directory: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/results/batch_full_pipeline2
======================================================================
  015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG | OK     |   9 dets | ['TUA', 'UXV', 'SNOC9SIA', 'VL-NOC9SI3', 'NO9SIA', 'NO9ST'] | 4.11s
  IMG20250416153020.jpg                         | OK     |   7 dets | ['BXV', 'NOC9SIY', '119S1', 'MXA', '30-1', '31SA'] | exact=0% partial=0% | 3.14s
  IMG20250416160821.jpg                         | OK     |   8 dets | ['5ST', 'BXY', 'NOSIC', '7-OE95', 'NCDO'] | exact=0% partial=0% | 3.04s
  IMG_0422.jpg                                  | OK     |  16 dets | ['30N', '31SB', '3IS630N', '30N-4', 'ISB', 'B1S530N'] | exact=0% partial=100% | 2.98s
  IMG_0771.jpg                                  | OK     |   5 dets | ['EUDYSTS', 'SHNOLGSA', '5140N2'] | exact=0% partial=0% | 2.69s
  IMG_0787.jpg                                  | OK     |   2 dets | ['B7S630N-11', 'AXA'] | exact=0% partial=100% | 2.40s
  IMG_1536.jpg                                  | OK     |   1 dets | [] | exact=0% partial=0% | 2.39s
  IMG_1537.jpg                                  | OK     |   2 dets | ['C-N09S'] | exact=0% partial=0% | 2.44s
  IMG_1548.jpg                                  | OK     |   2 dets | [] | exact=0% partial=0% | 1.33s
  IMG_1571.jpg                                  | OK     |   4 dets | ['SFNOE9SIS', 'TNOASTS'] | exact=0% partial=0% | 2.89s
  M4sb29-2.jpg                                  | OK     |   2 dets | ['6-5795'] | exact=0% partial=0% | 1.97s
  M5sb24-10.jpg                                 | OK     |   3 dets | ['9ITA95AW', 'KOOOTAAK'] | exact=0% partial=0% | 2.35s
  N5G15-X1Y1.jpg                                | OK     |   2 dets | ['XIX-GG', 'N7IIV'] | exact=0% partial=0% | 2.10s
  multi_001_sunny_yellow.JPG                    | OK     |  22 dets | ['IGNO9SIS', '1018', 'NO9S7', 'J-NOE9SI', '30N-34', 'IPXY', 'NCYSI2', 'BXY', '16X1', 'H-NU8', 'PSS', 'V10X', 'ID1A', 'SMYSTS', 'ALN', 'SH3ON-3'] | exact=0% partial=12% | 5.37s
  multi_008_yellow_green.JPG                    | OK     |   1 dets | ['NOR'] | exact=0% partial=0% | 2.80s
======================================================================
Summary saved to: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/results/batch_full_pipeline2/_summary.json
```


## Assistant

Now 14 out of 15 images have labels matched (the `015_B15b30N...` one still fails because the CSV has `b015_B15b30N-41__B15b30N-7A_45deg_NW_yellow` but the image is `015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG` — the `b` prefix removal only works when followed by digits, but here it's `b015` which should become `015`). Let me fix that:

**Tool Call: Edit OCR/scripts/batch\_full\_pipeline2.py**
Status: Completed

Diff: /mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/batch_full_pipeline2.py
```
    if not Path(name).suffix:
        name += ".jpg"
    # 先頭の余分な文字を修正
    # b015_ → 015_ (b + 数字で始まる場合)
    if name.startswith('b') and len(name) > 1 and name[1].isdigit():
        name = name[1:]
    # II → I (IIMG → IMG)
    if name.startswith('II') and not name.startswith('III'):
```


## Assistant

Now let me also check the `015` image — the CSV has `b015_B15b30N-41__B15b30N-7A_45deg_NW_yellow` and the image is `015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG`. After removing `b` we get `015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.jpg`. The stem should match. Let me verify:

**Tool Call: Terminal**
Status: Pending

Terminal:
```






```


**Tool Call: python3 -c "
from pathlib import Path
csv_name = 'b015_B15b30N-41__B15b30N-7A_45deg_NW_yellow'
img_name = '015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG'

# After normalization
csv_norm = csv_name.lower()
if csv_norm.startswith('b') and len(csv_norm) > 1 and csv_norm[1].isdigit():
    csv_norm = csv_norm[1:]
csv_norm += '.jpg'

img_norm = img_name.lower()

print(f'CSV normalized: {csv_norm}')
print(f'Image normalized: {img_norm}')
print(f'Stem CSV: {Path(csv_norm).stem}')
print(f'Stem IMG: {Path(img_norm).stem}')
print(f'Match: {Path(csv_norm).stem == Path(img_norm).stem}')
"**
Status: Completed

Terminal:
```
CSV normalized: 015_b15b30n-41__b15b30n-7a_45deg_nw_yellow.jpg
Image normalized: 015_b15b30n-41__b15b30n-7a_45deg_nw_yellow.jpg
Stem CSV: 015_b15b30n-41__b15b30n-7a_45deg_nw_yellow
Stem IMG: 015_b15b30n-41__b15b30n-7a_45deg_nw_yellow
Match: True

```


## Assistant

Good, the matching should work now. Let me re-run to confirm all 15 images get labels:

**Tool Call: python3 scripts/batch_full_pipeline2.py 2>&1 | grep -E "(WARNING|Loaded labels|Images with|Exact:|Partial:|Average)"**
Status: Completed

Terminal:
```
  Loaded labels for 15 images
     Exact: 0% (0/4), Partial: 0%
     Exact: 0% (0/2), Partial: 0%
     Exact: 0% (0/2), Partial: 0%
     Exact: 0% (0/3), Partial: 100%
     Exact: 0% (0/3), Partial: 0%
     Exact: 0% (0/1), Partial: 100%
     Exact: 0% (0/2), Partial: 0%
     Exact: 0% (0/4), Partial: 0%
     Exact: 0% (0/1), Partial: 0%
     Exact: 0% (0/2), Partial: 0%
     Exact: 0% (0/1), Partial: 0%
     Exact: 0% (0/1), Partial: 0%
     Exact: 0% (0/1), Partial: 0%
     Exact: 0% (0/8), Partial: 12%
     Exact: 0% (0/5), Partial: 0%
Images with labels: 15
Average Exact Match Rate: 0.0%
Average Partial Match Rate: 14.2%
```


## Assistant

All 15 images now have labels matched. The script is working correctly. Let me now provide the analysis report.## 結果報告

`batch_full_pipeline2.py` を作成・実行完了しました。

### スクリプトの機能
- **回転バウンディングボックス対応**: `cv2.minAreaRect` で検出した角度で画像を回転→クロップし、斜めテキストを正確に認識
- **使用可能文字フィルタ**: 英数字・`-+/._ ` のみ通過（モデルが出力する `?` 等の不正文字を除去）
- **正解ラベル照合**: CSVのファイル名揺れ（`b015_`→`015_`, `II`→`I`, ディレクトリプレフィックス除去）を自動修正してマッチング
- **精度指標**: 完全一致率 + 部分一致率（編集距離70%以上）を画像別・全体で集計
- **出力先**: `results/batch_full_pipeline2/`

### 評価結果（15画像）

| 指標 | 値 |
|------|-----|
| 完全一致率（平均） | **0.0%** |
| 部分一致率（平均） | **14.2%** |
| 処理時間 | 42秒（15画像） |

### 問題分析

認識精度が極めて低い原因は **モデル自体の限界** です：

1. **信頼度が極めて低い**: 全検出領域の平均信頼度が 0.01〜0.02（通常 0.8 以上が期待値）
2. **en_PP-OCRv3_rec.onnx は英語特化モデル**: 日本語産業ラベルのフォント（ステンシル体、角張った欧文書体）に未学習
3. **文字の形状が崩壊**: `BISb30N-7A` → `bXV` や `NOC9SIY` など、文字単位で全然違う文字列に化ける

### 改善案

| 優先度 | 施策 | 期待効果 |
|--------|------|----------|
| 🔴 高 | **日本語産業ラベル特化モデルの導入**（またはファインチューニング） | 根本解決 |
| 🟡 中 | **前処理の強化**: コントラスト強調・二値化・ノイズ除去 | 若干の改善 |
| 🟡 中 | **PaddleOCR2-by-nogami.py の 4方向回転投票** | 傾きテキストの改善 |
| 🟢 低 | **後処理**: 正解ラベル辞書との最近一致マッチング | 部分一致率向上 |

スクリプトは `OCR/scripts/batch_full_pipeline2.py` にあり、いつでも再実行可能です。モデルを差し替えた場合も、同じスクリプトで即座に再評価できます。

## Plan

- [x] Create batch_full_pipeline2.py with rotated crop + label comparison
- [x] Run batch_full_pipeline2.py and verify results
- [x] Review accuracy metrics and report
## User

出力画像を確認しましたが、BBoxの範囲が狭いです。
[@verify_detection_angle.py](file:///mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/verify_detection_angle.py) の時と同じ位の範囲です。文字に対してBBoxが狭いので、精度が悪いのも納得です。
[@verify_full_pipeline.py](file:///mnt/c/Users/soyak/StudioProjects/crossvision/OCR/scripts/verify_full_pipeline.py) コチラの実行結果のBBoxの範囲が理想です。
バウンディングボックスの範囲を調整して再度検証して欲しいです。
現在は「BBoxが狭い＝文字領域として検出している範囲が狭い」ので、十分な文字領域を取得できておらず不十分な文字領域に対してOCRをしているので、精度が悪いのではないでしょうか？
PaddleOCRは主に「検出（Detection）」「方向分類（Classification）」「認識（Recognition）」の3段階で構成されていると調べたら出てきました。
それぞれを明示的にわかるようにしてほしいですね。Dtectionが領域抽出ですよね？あ、det.onnxがそれか。んでen_PP-OCRv3_rec.onnxが認識か
つまりclasificationはないのかな？でも角度検出してるよね？
それぞれ３つのモデルに分けて、それぞれを呼び出すようにしましょう。
それぞれの機能をちゃんと人間が視覚的に理解できるようにして欲しいです。
あと、検出した角度？とか文字列を赤文字で表示してくれているけど、５倍くらい大きくしてくれてもいいかも。・小さくてわからない。
そうか、全部視覚的にわかりやすくなっているか。detectionがBBox、classificationが角度（赤文字）、Recognisionが抽出文字（赤文字）となっているか。
じゃあ今のままで進めて欲しいです。
修正してほしい内容は、
1. BBoxをもう少し大きくし、文字として認識している範囲を広くする
2. 赤文字ももう少し大きくしてほしい。
この２点を修正して再検証して欲しいです。
修正して再検証実行して
