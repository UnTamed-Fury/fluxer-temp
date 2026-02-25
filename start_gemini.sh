#!/bin/bash
# Startup script for Gemini with expanded memory to prevent crashes
export NODE_OPTIONS="--max-old-space-size=1500"
echo "Starting Gemini with expanded memory allocation (1500MB)..."
gemini
