# BandwidthMinimization

The bandwidth problem takes as input a graph G, with n vertices and m edges (ie. pairs of vertices). The goal is to find a permutation of the vertices on the line which minimizes the maximum length of any edge. This is better understood through an example. Suppose G consists of 5 vertices and the edges (v1, v2), (v1,v3), (v1,v4), and (v1,v5). We must map each vertex to a distinct number between 1 and n. Under the mapping vi → i, the last edge spans a distance of four (ie. 5-1). However, the permutation {2, 3, 1, 4, 5} is better, for none of the edges spans a distance of more than two. In fact, it is a permutation which minimizes the maximum length of the edges in G, as is any permutation where 1 is in the center.
  
The bandwidth problem has a variety of applications, including optimizing memory usage in hypertext documents. What is known about it? The problem is NP-complete, meaning that it is exceedingly unlikely that you will be able to find an algorithm with polynomial worst-case running time. It remains NP-complete even for restricted classes of trees. However, since the goal of the problem is to find a permutation, a backtracking program which iterates through all the n! possible permutations and computes the length of the longest edge for each gives an easy O(n! · m) algorithm. But the goal of this assignment is to find as practically good an algorithm as possible.

  
## Implementation
  
- Graph is represented as adjacency matrix in a 2D boolean array. If there is an edge between u and v, graph[u][v] = graph[v][u] = true;
- The lower-bound for bandwidth is at least half the degree of any vertex. Once a lower-bound bandwidth permutation is discovered, the program ends.
- In most cases the upper-bound for bandwidth would be the highest degree of a vertex. The permutation process starts from upperBound + 1, and to find optimal solution. If no solution can be found between lowerBound and upperBound - 1, then try to find a solution between upperBound and numVertex - 1. 
- Due to the fact that I am only looking for a possible better solution. The last branch is skipped, because its permutations were computed reversely earlier.
- If a better solution is encountered, replace the old one. If no solution can be found, the result array would be null.
- To speed up the permutation process, I also use multi-threading, to split the work.
