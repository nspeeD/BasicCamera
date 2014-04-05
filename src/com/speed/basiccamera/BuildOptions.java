package com.speed.basiccamera;

public class BuildOptions {
	//Log Levels
	public static int LOGLEVEL = 0;
	public static boolean VERBOSE = LOGLEVEL == 0;
	public static boolean DEBUG = LOGLEVEL <= 1;
 	public static boolean INFO = LOGLEVEL <= 2;
	public static boolean WARN = LOGLEVEL <= 3;
	public static boolean ERROR = LOGLEVEL <= 4;
}