#!/bin/bash
# Gemini Watchdog - Restarts on crash with 1500MB memory
export NODE_OPTIONS="--max-old-space-size=1500"
while true; do
    echo "[$(date)] Starting Gemini CLI..."
    gemini --prompt "I have just recovered from a crash. Reading FLUXER_STATE.md to resume the 6-hour build cycle. Continue the roadmap now."
    echo "[$(date)] Gemini CLI exited with code $?. Restarting in 5 seconds..."
    sleep 5
done
