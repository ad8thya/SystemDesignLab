# Dataset source

The `src/main/resources/search-terms.csv` file is a deterministic random subset of 500 English-language queries derived from CMU's published AOL query subset:

https://boston.lti.cs.cmu.edu/Data/web08-bst/planning.html

CMU selected frequent AOL queries and a frequency-weighted random sample. The source file's final column is the number of times a query appeared in the AOL query log.

For this lab copy, queries were URL-decoded and lower-cased. URL-like entries, entries containing digits, and entries matching a small list of explicit-content terms were excluded before selecting the subset. The original occurrence count is retained as `frequency`.

The data is historical (2006) and is used only to demonstrate Trie prefix matching, top-K ranking, and Redis caching. It must not be treated as current search popularity.
