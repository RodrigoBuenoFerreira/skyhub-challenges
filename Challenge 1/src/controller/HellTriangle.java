package controller;

import java.util.ArrayList;
import java.util.HashMap;

//intended solution: solve from bottom to top. start at the second to last row, judge the maximum value
//obtainable by every node in it, update the node's value from its single component to the composed new one, 
//save the path to the maximum value, move up a row and repeat
//in case of equal results for both options store both paths
public class HellTriangle {
	
	//hashmap to store the paths to the answers. using hashmap of arraylists for the 
	//possibility of storing a variable number of paths to the same answer (e.g.: [[1], [1,1]])
	//strings used for coordinates to avoid max triangle size troubles with base 10 numbers
	//and to avoid making it an arraylist of another list
	HashMap<Integer, ArrayList<String>> pathsMap = new HashMap<Integer, ArrayList<String>>();
	
	public void solve(int[][] input){
		
		//making a copy of the input to work with so the original input remains to use for the final answer
		int[][] triangle = new int[input.length][];
		for(int i = 0; i < input.length; i++){
			triangle[i] = input[i].clone();
		}
		
		//initializing the paths arraylists to avoid null pointers
		//one entry per node in the lowest level of the triangle to move up from
		for(int i = 0; i < triangle[triangle.length-1].length; i++){
			ArrayList<String> list = new ArrayList<String>();
			list.add("");
			pathsMap.put(i, list);
		}
		
		//loop through the levels of the triangle, going upwards, starting at the second to last and 
		//ending at the first, then through each node of the level
		for(int i = triangle.length-2; i >= 0; i--){
			HashMap<Integer, ArrayList<String>> newPathsMap = new HashMap<Integer, ArrayList<String>>();
			for(int j = 0; j < triangle[i].length; j++){
				int maxValue;
				ArrayList<String> newPathsList = new ArrayList<String>();
				
				//find what's the highest reachable value on the next level
				
				//check which reachable value is higher
				if(triangle[i+1][j] > triangle[i+1][j+1]){
					//store the maximum total value for the subtriangle started at the current node
					maxValue = triangle[i][j]+triangle[i+1][j];
					//append the current coordinates to the 
					for(String s : pathsMap.get(j)){
						if(!s.equals(""))
							s += ",";
						newPathsList.add(s+input[i+1][j]);
					}
				}
				
				
				else if(triangle[i+1][j] < triangle[i+1][j+1]){
					maxValue = triangle[i][j]+triangle[i+1][j+1];
					for(String s : pathsMap.get(j+1)){
						if(!s.equals(""))
							s += ",";
						newPathsList.add(s+input[i+1][j+1]);
					}
				}
				
				else{
					maxValue = triangle[i][j]+triangle[i+1][j];
					for(String s : pathsMap.get(j)){
						if(!s.equals(""))
							s += ",";
						newPathsList.add(s+input[i+1][j]);
					}
					for(String s : pathsMap.get(j+1)){
						if(!s.equals(""))
							s += ",";
						newPathsList.add(s+input[i+1][j+1]);
					}
				}

				triangle[i][j] = maxValue;
				newPathsMap.put(j, newPathsList);
				
			}
			pathsMap = newPathsMap;
		}
		
		System.out.println("Maximum total: " + triangle[0][0] + "\nPaths:");
		
		//append the first value and reverse the path string to reflect the top-down order of the triangle
		for(String path : pathsMap.get(0)){
			String[] substrings = path.split(",");
			String pretty = input[0][0]+"";
			for(int i = substrings.length-1; i >= 0; i--)
				pretty += " "+substrings[i];
			System.out.println(pretty);
		}
		System.out.println();
	}
	
	
}
