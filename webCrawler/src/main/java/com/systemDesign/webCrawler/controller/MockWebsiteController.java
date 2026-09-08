package com.systemDesign.webCrawler.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mock-web")
public class MockWebsiteController {

    private String buildPage(String title, String heading, String description, String linksHtml) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>%s</title>
            </head>
            <body>
                <h1>%s</h1>
                <p>%s</p>
                <nav>
                    <h3>Navigation Links</h3>
                    <ul>
                        %s
                    </ul>
                </nav>
            </body>
            </html>
            """.formatted(title, heading, description, linksHtml);
    }

    @GetMapping(value = "/home", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return buildPage(
            "Mock Store - Home",
            "Welcome to the Mock Store",
            "Seed page for automated web crawler testing. Discover multiple interlinked pages.",
            """
            <li><a href="/mock-web/about">About Us</a></li>
            <li><a href="/mock-web/products">Products Catalog</a></li>
            <li><a href="/mock-web/blog">Latest News & Blog</a></li>
            <li><a href="/mock-web/contact">Contact Support</a></li>
            <li><a href="/mock-web/assets/brochure.pdf">Download Brochure (PDF - should be filtered)</a></li>
            <li><a href="#top">Anchor link (should be filtered)</a></li>
            """
        );
    }

    @GetMapping(value = "/about", produces = MediaType.TEXT_HTML_VALUE)
    public String about() {
        return buildPage(
            "Mock Store - About Us",
            "About Our Company",
            "Learn more about our team and mission. Notice links leading back to home to test cycle prevention.",
            """
            <li><a href="/mock-web/home">Back to Home (Circular link test)</a></li>
            <li><a href="/mock-web/team">Meet the Team</a></li>
            <li><a href="/mock-web/contact">Contact Us</a></li>
            """
        );
    }

    @GetMapping(value = "/team", produces = MediaType.TEXT_HTML_VALUE)
    public String team() {
        return buildPage(
            "Mock Store - Leadership Team",
            "Our Leadership Team",
            "Meet our engineering and system design researchers.",
            """
            <li><a href="/mock-web/about">Back to About</a></li>
            <li><a href="/mock-web/home">Home</a></li>
            """
        );
    }

    @GetMapping(value = "/products", produces = MediaType.TEXT_HTML_VALUE)
    public String products() {
        return buildPage(
            "Mock Store - Products",
            "Product Catalog",
            "Explore our range of technology products.",
            """
            <li><a href="/mock-web/products/laptop">High Performance Laptop</a></li>
            <li><a href="/mock-web/products/phone">Flagship Smartphone</a></li>
            <li><a href="/mock-web/products/tablet">Creative Tablet</a></li>
            <li><a href="/mock-web/home">Home</a></li>
            """
        );
    }

    @GetMapping(value = "/products/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String productDetail(@PathVariable String id) {
        return buildPage(
            "Mock Store - Product: " + id,
            "Product Details: " + id.toUpperCase(),
            "Detailed specifications and pricing for " + id + ".",
            """
            <li><a href="/mock-web/products">Back to Catalog</a></li>
            <li><a href="/mock-web/cart">Add to Cart</a></li>
            <li><a href="/mock-web/home">Home</a></li>
            """
        );
    }

    @GetMapping(value = "/cart", produces = MediaType.TEXT_HTML_VALUE)
    public String cart() {
        return buildPage(
            "Mock Store - Shopping Cart",
            "Your Cart",
            "Review items in your shopping cart before checkout.",
            """
            <li><a href="/mock-web/products">Continue Shopping</a></li>
            <li><a href="/mock-web/home">Home</a></li>
            """
        );
    }

    @GetMapping(value = "/blog", produces = MediaType.TEXT_HTML_VALUE)
    public String blog() {
        return buildPage(
            "Mock Store - Blog",
            "Tech Insights & System Design",
            "Articles on distributed caching, Redis, and scalable web crawlers.",
            """
            <li><a href="/mock-web/blog/post-1">Article 1: Why Redis Sets for Deduplication?</a></li>
            <li><a href="/mock-web/blog/post-2">Article 2: Queue-based BFS Crawling</a></li>
            <li><a href="/mock-web/home">Home</a></li>
            """
        );
    }

    @GetMapping(value = "/blog/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    public String blogPost(@PathVariable String slug) {
        return buildPage(
            "Mock Store - Article: " + slug,
            "Reading: " + slug,
            "Full article content explaining modern architectural patterns for web crawling.",
            """
            <li><a href="/mock-web/blog">Back to Blog</a></li>
            <li><a href="/mock-web/about">About the Author</a></li>
            <li><a href="/mock-web/home">Home</a></li>
            """
        );
    }

    @GetMapping(value = "/contact", produces = MediaType.TEXT_HTML_VALUE)
    public String contact() {
        return buildPage(
            "Mock Store - Contact",
            "Get In Touch",
            "Customer support and queries.",
            """
            <li><a href="/mock-web/home">Home</a></li>
            <li><a href="/mock-web/about">About</a></li>
            """
        );
    }
}
