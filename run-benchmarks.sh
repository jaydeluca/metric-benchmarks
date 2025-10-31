#!/bin/bash
# Convenience wrapper for running benchmarks and updating README

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check if Python 3 is available
if ! command -v python3 &> /dev/null; then
    echo "Error: python3 is required but not found in PATH"
    exit 1
fi

# Run the Python script
exec python3 run-benchmarks.py "$@"
