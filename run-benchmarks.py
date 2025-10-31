#!/usr/bin/env python3
"""
Run JMH benchmarks and update README with formatted results.
"""

import subprocess
import re
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Tuple


def run_benchmarks():
    """Run JMH benchmarks using Gradle."""
    print("Running benchmarks... This may take several minutes.")
    try:
        result = subprocess.run(
            ["./gradlew", "jmh"],
            cwd=Path(__file__).parent,
            capture_output=True,
            text=True,
            timeout=3600  # 1 hour timeout
        )

        if result.returncode != 0:
            print(f"Benchmark execution failed: {result.stderr}")
            return False

        print("Benchmarks completed successfully!")
        return True
    except subprocess.TimeoutExpired:
        print("Benchmark execution timed out!")
        return False
    except Exception as e:
        print(f"Error running benchmarks: {e}")
        return False


def parse_results(results_file: Path) -> Dict[str, Dict[str, str]]:
    """Parse JMH results file and extract benchmark metrics."""
    if not results_file.exists():
        print(f"Results file not found: {results_file}")
        return {}

    results = {}

    with open(results_file, 'r') as f:
        lines = f.readlines()

    for line in lines[1:]:  # Skip header
        line = line.strip()
        if not line:
            continue

        parts = line.split()
        if len(parts) < 4:
            continue

        benchmark_name = parts[0]
        score = parts[2]

        # Extract main benchmark name (without gc metrics)
        if ':' in benchmark_name:
            main_name, metric = benchmark_name.split(':', 1)

            if main_name not in results:
                results[main_name] = {}

            # Store specific metrics
            if metric == '·gc.alloc.rate.norm':
                results[main_name]['alloc'] = score
            elif metric == '·gc.count':
                results[main_name]['gc_count'] = score
        else:
            # Main throughput metric
            if benchmark_name not in results:
                results[benchmark_name] = {}
            results[benchmark_name]['score'] = score
            results[benchmark_name]['units'] = parts[3] if len(parts) > 3 else 'ops/s'

    return results


def format_number(value: str) -> str:
    """Format numbers for better readability."""
    if not value or value == 'N/A':
        return 'N/A'

    # Handle special values
    if '≈' in value or value in ['0', '0.0', '0.00']:
        return '~0'

    try:
        num = float(value.replace(',', ''))
        if num >= 1_000_000_000:
            return f"{num / 1_000_000_000:.2f}B"
        elif num >= 1_000_000:
            return f"{num / 1_000_000:.2f}M"
        elif num >= 1_000:
            return f"{num / 1_000:.2f}K"
        else:
            return f"{num:.2f}"
    except (ValueError, AttributeError):
        return '~0' if '≈' in str(value) else str(value)


def group_benchmarks(results: Dict[str, Dict[str, str]]) -> Dict[str, List[Tuple[str, Dict[str, str]]]]:
    """Group benchmarks by type."""
    groups = {
        'Counter': [],
        'Gauge': [],
        'Timer': [],
        'Histogram': []
    }

    for name, metrics in results.items():
        if 'CounterBenchmark' in name:
            # Extract library name
            lib_name = name.replace('CounterBenchmark.', '')
            groups['Counter'].append((lib_name, metrics))
        elif 'GaugeBenchmark' in name:
            lib_name = name.replace('GaugeBenchmark.', '')
            groups['Gauge'].append((lib_name, metrics))
        elif 'TimerBenchmark' in name:
            lib_name = name.replace('TimerBenchmark.', '')
            groups['Timer'].append((lib_name, metrics))
        elif 'HistogramBenchmark' in name:
            lib_name = name.replace('HistogramBenchmark.', '')
            groups['Histogram'].append((lib_name, metrics))
        elif 'TimerAllocations' in name:
            # Legacy support for old benchmark names
            lib_name = name.replace('TimerAllocations.', '')
            # Try to categorize by name
            if 'timer' in lib_name.lower():
                groups['Timer'].append((lib_name, metrics))
            else:
                groups['Histogram'].append((lib_name, metrics))

    return groups


def clean_library_name(name: str) -> str:
    """Clean up library names for display."""
    # Handle exact matches first (highest priority)
    exact_matches = {
        # Prometheus variants
        'prometheusSimple': 'Prometheus (old client)',
        'prometheus': 'Prometheus (new client)',
        'simplePrometheusWithLabels': 'Prometheus (old) + labels',
        'prometheusWithLabels': 'Prometheus (new) + labels',
        'prometheusSimpleHistogram': 'Prometheus (old) Histogram',
        'prometheusHistogram': 'Prometheus (new) Histogram',
        # Old benchmark names from results
        'timers': 'Dropwizard Timer',
        'histogram': 'Dropwizard Histogram',
    }

    if name in exact_matches:
        return exact_matches[name]

    # Handle pattern-based matches (check these before generic replacements)
    if name.startswith('dropwizard'):
        # In separated benchmarks, we don't need the type suffix
        # But keep support for old combined benchmarks
        if 'Histogram' in name or 'histogram' in name:
            return 'Dropwizard'  # Type is now in section header
        elif 'Timer' in name or 'timer' in name:
            return 'Dropwizard'  # Type is now in section header
        return 'Dropwizard'

    if name.startswith('micrometer'):
        base = 'Micrometer'
        # Don't add type suffix for separated benchmarks
        # Type is in the section header
        if 'WithTags' in name or 'Tags' in name:
            return f"{base} + tags"
        return base

    if name.startswith('otel'):
        base = 'OpenTelemetry'
        # Don't add type suffix for separated benchmarks
        # Type is in the section header
        if 'WithAttributes' in name or 'Attributes' in name:
            return f"{base} + attributes"
        return base

    # Fallback: return as-is but capitalize first letter
    return name[0].upper() + name[1:] if name else name


def create_markdown_table(benchmarks: List[Tuple[str, Dict[str, str]]]) -> str:
    """Create a markdown table for a group of benchmarks with nice formatting."""
    if not benchmarks:
        return ""

    # Sort by score (throughput) descending
    def get_sort_key(x):
        score = x[1].get('score', '0')
        try:
            return float(score.replace(',', ''))
        except (ValueError, AttributeError):
            return 0

    sorted_benchmarks = sorted(benchmarks, key=get_sort_key, reverse=True)

    # Prepare data and calculate column widths
    rows = []
    for name, metrics in sorted_benchmarks:
        score = format_number(metrics.get('score', 'N/A'))
        alloc = format_number(metrics.get('alloc', 'N/A'))
        gc_count = format_number(metrics.get('gc_count', 'N/A'))
        display_name = clean_library_name(name)
        rows.append([display_name, score, f"{alloc} B", gc_count])

    # Calculate max width for each column
    headers = ["Library", "Throughput (ops/s)", "Allocation per op", "GC Count"]
    col_widths = [len(h) for h in headers]

    for row in rows:
        for i, cell in enumerate(row):
            col_widths[i] = max(col_widths[i], len(str(cell)))

    # Build table with proper spacing
    def format_row(cells, align):
        """Format a row with proper alignment."""
        formatted = []
        for i, cell in enumerate(cells):
            width = col_widths[i]
            if align[i] == 'left':
                formatted.append(cell.ljust(width))
            else:
                formatted.append(cell.rjust(width))
        return "| " + " | ".join(formatted) + " |"

    # Header alignment: left for library name, right for numbers
    align = ['left', 'right', 'right', 'right']

    # Build table
    table = format_row(headers, align) + "\n"

    # Separator row
    sep = []
    for i, width in enumerate(col_widths):
        if align[i] == 'left':
            sep.append(' ' + '-' * width + ' ')
        else:
            sep.append(' ' + '-' * (width - 1) + ': ')
    table += "|" + "|".join(sep) + "|\n"

    # Data rows
    for row in rows:
        table += format_row(row, align) + "\n"

    return table


def update_readme(grouped_results: Dict[str, List[Tuple[str, Dict[str, str]]]]):
    """Update README.md with benchmark results."""
    readme_path = Path(__file__).parent / "readme.md"

    # Read existing README
    if readme_path.exists():
        with open(readme_path, 'r') as f:
            content = f.read()
    else:
        content = "# Metric Benchmarks\n\n"

    # Generate benchmark results section
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    results_section = f"\n## Benchmark Results\n\n"
    results_section += f"*Last updated: {timestamp}*\n\n"
    results_section += "Run benchmarks with: `./run-benchmarks.py` or `./gradlew jmh`\n\n"

    for group_name, benchmarks in grouped_results.items():
        if benchmarks:
            results_section += f"### {group_name} Benchmarks\n\n"
            results_section += create_markdown_table(benchmarks)
            results_section += "\n"

    results_section += "**Notes:**\n"
    results_section += "- Throughput: Higher is better (operations per second)\n"
    results_section += "- Allocation per op: Lower is better (bytes allocated per operation)\n"
    results_section += "- GC Count: Lower is better (number of garbage collections during test)\n"
    results_section += "- B = Billion, M = Million, K = Thousand\n\n"

    # Remove old results section if it exists
    pattern = r'\n## Benchmark Results\n.*?(?=\n## |\Z)'
    content = re.sub(pattern, '', content, flags=re.DOTALL)

    # Find where to insert (before ## Clickhouse or at the end)
    clickhouse_pos = content.find('\n## Clickhouse')
    if clickhouse_pos != -1:
        content = content[:clickhouse_pos] + results_section + content[clickhouse_pos:]
    else:
        # Append at the end
        content = content.rstrip() + '\n' + results_section

    # Write updated README
    with open(readme_path, 'w') as f:
        f.write(content)

    print(f"README.md updated successfully!")


def main():
    """Main entry point."""
    print("=" * 60)
    print("JMH Benchmark Runner & Results Updater")
    print("=" * 60)
    print()

    # Check if we should skip running benchmarks
    skip_run = '--skip-run' in sys.argv

    if not skip_run:
        if not run_benchmarks():
            print("Failed to run benchmarks. Exiting.")
            sys.exit(1)
    else:
        print("Skipping benchmark execution (--skip-run provided)")

    print()
    print("Parsing results...")

    results_file = Path(__file__).parent / "build" / "results" / "jmh" / "results.txt"
    results = parse_results(results_file)

    if not results:
        print("No results found to parse!")
        sys.exit(1)

    print(f"Parsed {len(results)} benchmark results")

    print("Grouping and formatting results...")
    grouped = group_benchmarks(results)

    print("Updating README.md...")
    update_readme(grouped)

    print()
    print("=" * 60)
    print("Done! Check readme.md for updated benchmark results.")
    print("=" * 60)


if __name__ == "__main__":
    main()
