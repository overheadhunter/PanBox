/*
 * 
 *               Panbox - encryption for cloud storage 
 *      Copyright (C) 2014-2015 by Fraunhofer SIT and Sirrix AG 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additonally, third party code may be provided with notices and open source
 * licenses from communities and third parties that govern the use of those
 * portions, and any licenses granted hereunder do not alter any rights and
 * obligations you may have under such open source licenses, however, the
 * disclaimer of warranty and limitation of liability provisions of the GPLv3 
 * will apply to all the product.
 * 
 */
package org.panbox.desktop.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

/**
 * @author Dominik Spychalski
 * 
 *         Provide functions to handle some processes and collect information of them.
 */

public class ProcessHandler {
private int ownPID;
	
	private ProcessHandler(){
		this.ownPID = Integer.valueOf(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
	}
	
	private static ProcessHandler instance; 
	
	public static ProcessHandler getInstance(){
		if(instance == null){
			instance = new ProcessHandler();
		}
		return instance;
	}
	
	//returns the PID of the own panbox instance
	public int getOwnPID(){
		return this.ownPID;
	}
	
	//kills a Process with a specific pid
	public boolean killProcess(int pid){
		boolean ret = false;
		boolean running = checkProcessRunning(pid);
		
		if(running){
			String[] args = new String[]{"kill", "-9", String.valueOf(pid)};
			this.executeProcess(args);
			
			running = checkProcessRunning(pid);
			
			if(running){
				ret = false;
			}else{
				ret = true;
			}
		}
		
		return ret;
	}
	
	//returns the pid of a the parent of a specific process
	public int getProcessPID(int processID){
		int ret = 0;
		String outputLine  = "";
		
		try {
			String[] args = new String[]{"ps", "-o", "ppid", String.valueOf(processID)};
			BufferedReader input = this.executeProcess(args);
			
			while((outputLine = input.readLine()) != null){
				if(!outputLine.toUpperCase().contains("PPID")){
					outputLine = outputLine.replace(" ", "");
					ret = Integer.valueOf(outputLine);
					break;
				}
			}
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return ret;
	}
	
	//returns a array of pid's of processes which match to the search pattern in searchCommand
	public ArrayList<Integer> getProcessID(String searchCommand){
		ArrayList<Integer> ret = new ArrayList<>(); 
		String outputLine = "";
		
		try {	
			String[] args = new String[]{"ps", "aux"};
			BufferedReader input = this.executeProcess(args);
			
			int pidIndex = -1;
			boolean loopInit = true;
			
			while((outputLine = input.readLine()) != null){
				
				if(loopInit){
					pidIndex = getPIDColumnIndex(outputLine);
					loopInit = false;
				}
				
				if(outputLine.contains(searchCommand)){
					int tmp = getPIDFromCoulumn(outputLine, pidIndex);
					
					if(tmp != this.getOwnPID()){
						ret.add(tmp);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	//checks if an specific process is running
	public boolean checkProcessRunning(int pid){
		boolean ret = false;
		String outputLine = "";
		
		try{
			String[] args = new String[]{"ps", "h", String.valueOf(pid)};
			BufferedReader input = this.executeProcess(args);
			
			while((outputLine = input.readLine()) != null){
				if(!outputLine.isEmpty()){
					ret = true;
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return ret;
	}
	
	//checks of a process matching to the search pattern in searchCommand is running
	public boolean checkProcessRunning(String searchCommand){
		String outputLine = "";
		boolean ret = false;
		
		int searchedProcessID = 0;
		
		try {
			String[] args = new String[]{"ps", "aux"};
			BufferedReader input = this.executeProcess(args);
			
			int pidIndex = -1;
			boolean loopInit = true;
			
			while((outputLine = input.readLine()) != null){
				
				if(loopInit){
					pidIndex = getPIDColumnIndex(outputLine);
					loopInit = false;
				}
				
				if(!loopInit && outputLine.contains(searchCommand)){
					searchedProcessID = getPIDFromCoulumn(outputLine, pidIndex);

					if(searchedProcessID != this.getOwnPID()){
						ret = true;
					}	
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public int getPIDColumnIndex(String outputLine){
		int ret = -1;
		String line = outputLine.toLowerCase();
		String[] parts = line.split("\\s+");
		int index = 0;
		
		for(int i = 0; i < parts.length; i++){
			
			if(parts[i].equals("pid")){
				ret = index;
			}
			index++;
		}
		
		return ret;
	}
	
	public int getPIDFromCoulumn(String outputLine, int index){
		String[] parts = outputLine.split("\\s+");

		return Integer.valueOf(parts[index]);
	}
	
	//executes a application in /bin/ and returns the terminal output
	public BufferedReader executeProcess(String[] args){
		Process p = null;
		BufferedReader ret = null;
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.directory(new File("/bin/"));
		
		try{
			p = pb.start();
			ret = new BufferedReader (new InputStreamReader(p.getInputStream()));
		}catch(Exception e){
			e.printStackTrace();
			ret = null;
		}
		
		return ret;
	}
	
	//Performs a process sleep, necessary for panbox directory unmount hierarchy
	public void sleep(){
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
