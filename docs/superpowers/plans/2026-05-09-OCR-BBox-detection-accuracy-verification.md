# OCR-BBox Detection Accuracy Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a verification script that processes 15 OCR images, filters recognized text by allowed characters, compares with correct labels, and calculates accuracy metrics.

**Architecture:** We'll create a new verification script that builds upon the existing batch_full_pipeline.py core logic, adding character filtering, label comparison, and accuracy calculation. The script will output results to results/batch_full_pipeline2/.

**Tech Stack:** Python, OpenCV, ONNX Runtime, NumPy, Pathlib, JSON, CSV, Regex

---