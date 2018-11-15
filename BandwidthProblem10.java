import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 * @author yifanwang98
 * @version 10
 * @submitted Nov 8, 2018
 *
 */
public class BandwidthProblem10 {

	static ExecutorService executor = Executors.newFixedThreadPool(2);
	static Lock lock1 = new ReentrantLock(); // Lock for first thread
	static Lock lock2 = new ReentrantLock(); // Lock for second thread
	static Lock lock = new ReentrantLock(); // Lock for updating result

	static boolean[][] graph; // Adjacency matrix for the graph
	static int numVertex, halfNumVertex, lowerBound; // Number of vertices
	static boolean lowerBoundReached = false; // Terminate searches when is true

	static Result r1 = new Result(); // Result object for first thread
	static Result r2 = new Result(); // Result object for second thread

	public static void main(String[] args) throws NumberFormatException, IOException {
		System.out.println("[Version 10]\n");

		// Obtain file path
		Scanner input = new Scanner(System.in);
		System.out.print("Enter File Path: ");
		final String filePath = input.nextLine();
		//final String filePath = "./datasetHW4/" + input.nextLine() + ".txt";
		input.close();

		final long startTime = System.currentTimeMillis();

		// Read file
		BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));

		// Get Data
		numVertex = Integer.parseInt(br.readLine());
		halfNumVertex = numVertex / 2;
		int numEdge = Integer.parseInt(br.readLine());

		// Make adjacency matrix of graph
		graph = new boolean[numVertex][numVertex];
		int[] edgeCount = new int[numVertex];
		int max = 0;
		String[] s;
		for (int i = 0; i < numEdge; i++) {
			s = br.readLine().split(" ");
			int u = Integer.parseInt(s[0]) - 1;
			int v = Integer.parseInt(s[s.length - 1]) - 1;
			graph[u][v] = true;
			graph[v][u] = true;
			edgeCount[u]++;
			edgeCount[v]++;
			if (edgeCount[u] > max)
				max = edgeCount[u];
			if (edgeCount[v] > max)
				max = edgeCount[v];
		}
		br.close();

		// Calculate possible bandwidth range
		int upperBound = max + 1;
		r2.bandwidth = r1.bandwidth = upperBound;
		final int increment = max / 2;
		lowerBound = increment;

		// Execution
		executor.execute(new FirstHalfTask());
		executor.execute(new SecondHalfTask());
		executor.shutdown();

		// Make sure all threads are finished
		lock1.lock();
		lock2.lock();
		lock2.unlock();
		lock1.unlock();

		int upper = upperBound + increment;
		lowerBound = upperBound;
		while (r1.current == null && r2.current == null) {
			if (lowerBound >= numVertex)
				break;
			r1.bandwidth = upper;
			r2.bandwidth = upper;

			executor = Executors.newFixedThreadPool(2);
			executor.execute(new FirstHalfTask());
			executor.execute(new SecondHalfTask());
			executor.shutdown();

			// Make sure all threads are finished
			lock1.lock();
			lock2.lock();
			lock2.unlock();
			lock1.unlock();
			upper += increment;
			lowerBound += increment;
		}

		if (r1.bandwidth > r2.bandwidth || r1.current == null) {
			r1 = r2;
		}
		if (r1.current != null) {
			for (int i = 0; i < numVertex; i++)
				r1.current[i] += 1;
		} else {
			System.out.println("No solution found.");
		}

		// Print Result
		final long endTime = System.currentTimeMillis();
		System.out.println("\nParallel time with " + Runtime.getRuntime().availableProcessors() + " processors is "
				+ (endTime - startTime) / 1000.0 + " seconds\n");

		System.out.printf("Permutation:\n%s\n\n", Arrays.toString(r1.current));
		System.out.printf("Bandwidth: %d", r1.bandwidth);
	}

	public static void permute(boolean[][] graph, boolean[] used, int[] list, int count, Result r, int len) {
		if (count == len) {
			// Further pruning
			int diff = len - r.bandwidth;
			int temp, i, j;
			boolean[] gI;
			for (i = 0; i < diff; i++) {
				gI = graph[list[i]];
				temp = i + r.bandwidth;
				for (j = len - 1; j >= temp; j--) {
					if (gI[list[j]]) {
						return;
					}
				}
			}

			// Update result
			lock.lock();
			int width = 0;
			for (i = 0; i < len - 1; i++) {
				gI = graph[list[i]];
				for (j = i + 1; j < len; j++) {
					if (gI[list[j]]) {
						width = j - i > width ? j - i : width;
					}
				}
			}

			if (width < r2.bandwidth) {
				r2.bandwidth = width;
				r2.current = list.clone();
			}
			if (r1.bandwidth > width) {
				r1.bandwidth = width;
				r1.current = list.clone();
			}
			lock.unlock();
			return;
		}

		if (lowerBoundReached)
			return;

		// Pruning (Most branches are cut off here)
		boolean[] b, set = new boolean[len];
		int c, oldSize, j, k, size = 0;
		for (j = 0; j < count; j++) {
			c = 0;
			oldSize = size;
			b = graph[list[j]];
			for (k = 0; k < len; k++) {
				if (!used[k] && b[k] && !set[k]) {
					set[k] = true;
					c++;
					size++;
				}
			}
			if (c > 0 && count - j + c + oldSize > r.bandwidth) {
				return;
			}
		}

		// Find better permutation
		int newCount = count + 1;
		for (int i = 0; i < len; i++) {
			if (!used[i]) {
				// Recursion
				list[count] = i;
				used[i] = true; // Mark used
				permute(graph, used, list, newCount, r, len);
				used[i] = false; // Mark unused
			}
		}
	}

	public static class FirstHalfTask implements Runnable {
		public void run() {
			lock1.lock();
			int len = numVertex;
			int[] arr = new int[len];
			boolean[] traversed = new boolean[len];
			for (int i = 0; i < halfNumVertex; i++) {
				arr[0] = i;
				traversed[i] = true;
				permute(graph, traversed, arr, 1, r1, len);
				traversed[i] = false;
				if (r1.bandwidth == lowerBound || lowerBoundReached)
					break;
			}
			if (r1.bandwidth == lowerBound)
				lowerBoundReached = true;
			lock1.unlock();
		}
	}

	public static class SecondHalfTask implements Runnable {
		public void run() {
			lock2.lock();
			int len = numVertex;
			int[] arr = new int[len];
			boolean[] traversed = new boolean[len];
			int upper = len - 1;
			for (int i = halfNumVertex; i < upper; i++) {
				arr[0] = i;
				traversed[i] = true;
				permute(graph, traversed, arr, 1, r2, len);
				traversed[i] = false;
				if (r2.bandwidth == lowerBound || lowerBoundReached)
					break;
			}
			if (r2.bandwidth == lowerBound)
				lowerBoundReached = true;
			lock2.unlock();
		}
	}
}

class Result {
	int[] current;
	int bandwidth;
}