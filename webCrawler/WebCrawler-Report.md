# Lab Exercise 8: Web Crawler for Automated Web Content Discovery

**Course:** UCS3513 System Design Laboratory  
**Department:** Computer Science and Engineering  
**Institution:** Sri Sivasubramaniya Nadar College of Engineering  

---

## 1. Objective
To design and develop a scalable, queue-based web crawler using Spring Boot for automated web content discovery, manage URLs awaiting crawl efficiently via Redis Lists, eliminate duplicate crawling using a Redis Visited-URL Set ($O(1)$ lookup), cache extracted page metadata in Redis, expose crawler operations via REST APIs, and evaluate performance under concurrent workloads using JMeter.

---

## 2. System Architecture

```text
                        Client (Browser / Postman / JMeter)
                                       |
                                       v
                    Spring Boot REST API: /api/crawler/*
                                       |
                                       v
                             WebCrawlerService
                                       |
              +------------------------+------------------------+
              |                                                 |
              v                                                 v
      Page Retrieval & Parsing                         Redis State Store
      (Jsoup HTML Parser)                              (Spring Data Redis)
              |                                                 |
     1. Download HTML page                             1. crawler:queue (LIST)
     2. Extract <a href> hyperlinks                       FIFO crawl frontier
     3. Normalize & filter URLs                        2. crawler:visited (SET)
        - Scheme check (http/https)                       O(1) duplicate check
        - Exclude binary files (.pdf, .png)            3. crawler:discovered (SET)
        - Validate allowed domains HashSet                All uncovered URLs
                                                       4. crawler:pages (HASH)
                                                          Cached PageMetadata
```

---

## 3. Role of Core Architectural Components

### • Seed URL
* **Role**: The initial entry point provided to the crawler (e.g., `http://localhost:8080/mock-web/home` or `https://example.com`).
* **Function**: Placed into the URL queue at `depth = 0`. The crawler extracts hyperlinks from this page to bootstrap the entire discovery process.

### • URL Queue (`crawler:queue`)
* **Role**: Maintains the crawl frontier (the ordered collection of URLs waiting to be fetched).
* **Data Structure**: Implemented using a **Redis `LIST`** (`RPUSH` to enqueue, `LPOP` to dequeue).
* **Function**: Ensures **First-In, First-Out (FIFO)** processing, which produces a **Breadth-First Search (BFS)** traversal of the web graph.

### • Visited-URL Set (`crawler:visited`)
* **Role**: Prevents cyclic visits and duplicate crawling.
* **Data Structure**: Implemented using a **Redis `SET`** (`SADD`, `SISMEMBER`).
* **Function**: Offers **$O(1)$ time complexity** membership verification. Before fetching any page from the queue, the crawler queries Redis to check if it has already been processed.

### • Web Crawler (`WebCrawlerService`)
* **Role**: The execution engine driving the crawl cycle.
* **Function**: Continuously dequeues URLs, coordinates with the visited set, issues HTTP requests via **Jsoup**, parses DOM trees, extracts anchors, applies validation filters, and caches results.

### • Redis Cache (`crawler:pages`)
* **Role**: High-speed in-memory store for crawling state and page metadata.
* **Data Structure**: Implemented using a **Redis `HASH`** mapping `URL -> PageMetadata`.
* **Function**: Stores HTTP status, response latency, page title, and outgoing links. Serves repeated requests instantaneously without re-fetching content over the network.

---

## 4. Detailed Answers to Lab Analysis Questions

### Q1. Explain how the crawler discovers new URLs from a retrieved web page.
1. The crawler connects to the target URL using `Jsoup.connect(url)`.
2. Upon receiving an `HTTP 200 OK`, Jsoup parses the raw HTML string into an in-memory DOM document structure (`Document doc = response.parse()`).
3. The crawler executes a CSS query `doc.select("a[href]")` to locate all anchor tags that contain a hyperlink attribute.
4. Relative links (e.g. `/mock-web/about`) are converted into fully qualified absolute URLs (e.g. `http://localhost:8080/mock-web/about`) using Jsoup's `abs:href` resolver.
5. The extracted links are passed to the URL validation filter.

### Q2. Explain how the system determines whether a newly discovered URL should be crawled.
Every discovered URL must pass through a strict validation pipeline in `UrlFilterService`:
1. **Normalization**: Anchor fragments (`#section`) and redundant trailing slashes are stripped.
2. **Protocol Validation**: Only `http` and `https` schemes are accepted; browser pseudo-protocols (`javascript:`, `mailto:`, `tel:`) are discarded.
3. **MIME/Extension Filtering**: Non-HTML resources (`.pdf`, `.png`, `.jpg`, `.zip`, `.mp4`, `.css`, `.js`) are rejected.
4. **Domain Scoping**: The URL's host is checked against an **allowed domains `HashSet`**. If the host does not match the permitted domains, it is discarded to prevent the crawler from escaping to third-party domains.
5. **Depth & Duplicate Check**: If `currentDepth < maxDepth` and `!redisCrawlStateService.isVisited(url)`, the URL is approved and pushed to the queue.

### Q3. Explain how the system processes a URL that has not been visited.
1. The URL is popped from `crawler:queue`.
2. The crawler performs `SISMEMBER crawler:visited <url>`. Since it returns `false`, processing continues.
3. The crawler immediately marks the URL as visited via `SADD crawler:visited <url>` to prevent duplicate additions by concurrent workers.
4. An HTTP GET request is sent to download the HTML page and response latency is measured.
5. Hyperlinks are extracted, filtered, and unvisited links are enqueued into `crawler:queue` at `depth + 1`.
6. Extracted metadata (`PageMetadata`: title, HTTP status, outgoing links, latency) is stored in the Redis hash `crawler:pages`.

### Q4. Explain how the system processes a URL that has already been visited.
1. The URL is popped from `crawler:queue`.
2. The crawler performs `SISMEMBER crawler:visited <url>`.
3. Because the URL exists in the Redis Set, `isVisited()` returns `true`.
4. The crawler logs `Skipping already visited URL`, terminates further processing for this item, and immediately proceeds to dequeue the next URL.
5. **Impact**: No HTTP network connection is opened, no DOM parsing occurs, and circular reference infinite loops (e.g. $A \rightarrow B \rightarrow A$) are prevented.

### Q5. Compare queue-based URL crawling with repeatedly scanning the complete collection of URLs.

| Criteria | Queue-Based Crawling (BFS Frontier) | Repeatedly Scanning Complete Collection |
| :--- | :--- | :--- |
| **Traversal Order** | Predictable Breadth-First Search (BFS) level by level. | Uncontrolled or redundant linear scans. |
| **Time Complexity** | $O(V + E)$ where $V$ = unique URLs, $E$ = links. Each page crawled once. | $O(N^2)$ or unbounded, repeatedly re-evaluating known pages. |
| **Memory Utilization** | Memory tracks only the active frontier ($O(\text{Queue Size})$) and visited set ($O(V)$). | High memory footprint; must keep entire corpus active or repeatedly iterate full lists. |
| **Scalability** | High: Multiple crawler workers can consume from the distributed Redis queue concurrently. | Poor: Causes contention, lock conflicts, and duplicate network calls. |
| **Cycle Avoidance** | Natural $O(1)$ visited set lookup prevents infinite loops. | Prone to getting trapped in circular hyperlink cycles. |

### Q6. Analyze the performance of the crawler under different numbers of concurrent requests using JMeter.
*(To be completed by executing the provided `webcrawler-load-test.jmx` script in JMeter)*

#### Expected Behavior:
1. **With Redis Deduplication / Caching (`freshCrawl=false`)**:
   - Repeated crawl requests for previously crawled trees hit the Redis visited set and page cache.
   - Average response latency drops significantly (often from hundreds of milliseconds to under 10–20 ms).
   - Throughput (requests/sec) scales steadily with concurrent threads.
2. **Without Redis State / Cold Runs (`freshCrawl=true`)**:
   - Every request re-downloads and parses all HTML documents over HTTP.
   - Response latency increases and fluctuates depending on network and DOM parsing overhead.

---

## 5. How to Run the System

### Step 1: Start Redis using Docker
```bash
cd webCrawler
docker compose up -d
```

### Step 2: Run the Spring Boot Application
```bash
./mvnw spring-boot:run
```
*(The server starts on port `8080`)*

### Step 3: Test with Postman / cURL
* **Trigger Crawl (Mock Dataset)**:
  ```bash
  curl -X GET "http://localhost:8080/api/crawler/crawl?seedUrl=http://localhost:8080/mock-web/home&maxDepth=2&maxPages=20&freshCrawl=true"
  ```
* **Trigger Crawl (POST with JSON)**:
  ```bash
  curl -X POST "http://localhost:8080/api/crawler/crawl" \
       -H "Content-Type: application/json" \
       -d '{"seedUrl": "http://localhost:8080/mock-web/home", "maxDepth": 2, "maxPages": 20, "freshCrawl": true}'
  ```
* **Reset Redis Crawler State**:
  ```bash
  curl -X POST "http://localhost:8080/api/crawler/reset"
  ```

### Step 4: Run JMeter Performance Test
1. Open **Apache JMeter**.
2. File $\rightarrow$ Open $\rightarrow$ Select `webCrawler/jmeter/webcrawler-load-test.jmx`.
3. Click the green **Start** button (Run).
4. Inspect the **Summary Report** and **View Results Tree** listeners to record throughput, average response time, and error rate.
