package test;

import java.util.Random;

import controller.HellTriangle;

public class HellTriangleTest {
	
	//amount of random triangle tests to perform
	final static int RANDOM_TEST_COUNT = 5;
	
	//amount of lines each random triangle should have
	final static int RANDOM_TEST_DEPTH = 4;
	
	//maximum value for each node in the random triangle tests
	final static int RANDOM_TEST_MAX_VAL = 9;

	public static void main(String[] args) {
		
		int[][] testArray;
		HellTriangle triangle = new HellTriangle();
		
		System.out.println("Triangle proposed by the challenge\n");
		System.out.println("6\n3 5\n9 7 1\n4 6 8 4");
		
		testArray = new int[][]{{6},{3,5},{9,7,1},{4,6,8,4}};
		triangle.solve(testArray);
		
		System.out.println("Tests with randomly generated triangles\n");
		
		for(int k = 0; k < RANDOM_TEST_COUNT; k++){
			testArray = new int[RANDOM_TEST_DEPTH][];
			Random random = new Random();
			String pretty = "";
			
			for(int i = 0; i < RANDOM_TEST_DEPTH; i++){
				testArray[i] = new int[i+1];
				for(int j = 0; j <= i; j++){
					testArray[i][j] = random.nextInt(RANDOM_TEST_MAX_VAL+1);
					pretty += testArray[i][j] + " ";
				}
				pretty += "\n";
			}
			
			System.out.print("Test " + (k+1) + ":\n" + pretty);
			triangle.solve(testArray);
		}
	}

}
