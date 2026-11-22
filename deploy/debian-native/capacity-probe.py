#!/usr/bin/env python3
"""有界只读容量探测：并发拉取 ERP 静态首页，记录时延与错误率。

只做 GET /，不触碰登录、事务或任何写接口；并发与总请求数都有硬上限，
用于验证 2C2G 机器在预期并发下的响应能力，不构成压测认证。
"""
import argparse
import statistics
import sys
import threading
import time
import urllib.error
import urllib.request

RESULTS = []
LOCK = threading.Lock()


def worker(url, count, timeout):
    local = []
    for _ in range(count):
        start = time.perf_counter()
        code = 0
        try:
            with urllib.request.urlopen(url, timeout=timeout) as resp:
                resp.read()
                code = resp.status
        except urllib.error.HTTPError as error:
            code = error.code
        except Exception:
            code = 0
        local.append(((time.perf_counter() - start) * 1000.0, code))
    with LOCK:
        RESULTS.extend(local)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--url', default='http://127.0.0.1:8081/')
    parser.add_argument('--concurrency', type=int, default=5)
    parser.add_argument('--requests', type=int, default=50, help='每并发线程的请求数')
    parser.add_argument('--timeout', type=float, default=10.0)
    args = parser.parse_args()

    if not 1 <= args.concurrency <= 20:
        raise SystemExit('concurrency must be within 1..20')
    if not 1 <= args.requests <= 200:
        raise SystemExit('requests per worker must be within 1..200')

    total = args.concurrency * args.requests
    threads = [threading.Thread(target=worker, args=(args.url, args.requests, args.timeout))
               for _ in range(args.concurrency)]
    started = time.perf_counter()
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    elapsed = time.perf_counter() - started

    latencies = sorted(r[0] for r in RESULTS)
    ok = sum(1 for r in RESULTS if 200 <= r[1] < 300)
    failed = len(RESULTS) - ok
    codes = {}
    for _, code in RESULTS:
        codes[code] = codes.get(code, 0) + 1

    def pct(p):
        if not latencies:
            return None
        idx = min(len(latencies) - 1, int(round((p / 100.0) * (len(latencies) - 1))))
        return round(latencies[idx], 2)

    summary = {
        'url': args.url,
        'concurrency': args.concurrency,
        'total_requests': total,
        'elapsed_seconds': round(elapsed, 3),
        'throughput_rps': round(total / elapsed, 1) if elapsed > 0 else None,
        'success': ok,
        'failed': failed,
        'status_codes': codes,
        'latency_ms': {
            'min': round(latencies[0], 2) if latencies else None,
            'p50': pct(50), 'p95': pct(95), 'p99': pct(99),
            'max': round(latencies[-1], 2) if latencies else None,
            'mean': round(statistics.fmean(latencies), 2) if latencies else None,
        },
    }
    import json
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0 if failed == 0 else 1


if __name__ == '__main__':
    sys.exit(main())
