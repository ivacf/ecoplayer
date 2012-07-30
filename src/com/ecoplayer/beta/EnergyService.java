/*
 * Author:	Ivan Carballo Fernandez (icf1e11@soton.ac.uk) 
 * Project:	EcoPlayer - Battery-friendly music player for Android (MSc project at University of Southampton)
 * Date:	13-07-2012
 * License: Copyright (C) 2012 Ivan Carballo. 
 */
package com.ecoplayer.beta;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

/* This service performs energy related operations in a new thread. Two are the main actions that it can carry out: 
 * ACTION_SET_ENERGY_MODE: The service receives a EnergyMode object inside the intent and applies these settings to the system.
 * ACTION_GET_ENERGY_STATE: The service reads the energy settings from the system and saves them in the IntialEnergySettings object */
@SuppressLint("UseValueOf")
// It doesn't work using valueOf() (don't know why)
public class EnergyService extends IntentService {

	public EnergyService() {
		super("com.ecoplayer.beta.EnergyService");
	}

	public static final String EXTRA_ENERGY_MODE = "com.ecoplayer.beta.EXTRA_ENERGY_MODE";
	// Actions for the service
	public static final String ACTION_SET_ENERGY_MODE = "com.ecoplayer.beta.SET_ENERGY_MODE";
	public static final String ACTION_GET_ENERGY_STATE = "com.ecoplayer.beta.GET_ENERGY_STATE";
	// Actions for the broadcast
	public static final String ENERGY_MODE_SET = "com.ecoplayer.beta.ENERGY_MODE_SET ";
	public static final String ENERGY_STATE_GET = "com.ecoplayer.beta.ENERGY_STATE_GET";
	// List of available CPU frequencies supported by the device.
	private List<Integer> availableFrequencies = null;
	// List of available CPU governors supported by the device.
	private List<String> availableGovernors = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// ACTION_SET_ENERGY_MODE: The service receives a EnergyMode object
		// inside the intent and applies these settings to the system.
		if (intent.getAction().equals(ACTION_SET_ENERGY_MODE)) {
			EnergyMode energyMode = (EnergyMode) intent.getExtras().getSerializable(EXTRA_ENERGY_MODE);
			if (energyMode != null) {
				if (!energyMode.isAirPlaneModeOn()) {
					if (!energyMode.isBluetoothOn())
						disableBluetooth();
					setWifiEnabled(energyMode.isWifiOn());
				}
				setAutoSynEnabled(energyMode.isAutoSyncOn());
				setAirplaneModeOn(energyMode.isAirPlaneModeOn());
				int maxFreq = energyMode.getCPUFrequency();
				if (maxFreq != EnergyMode.NO_FREQUENCY)
					setCpuMaxFrequency(maxFreq);
				String governor = energyMode.getGovernor();
				if (governor != null)
					setCpuGovernor(governor);

			} else {
				Log.e(getClass().getName(), "There is not energy mode object inside the Intent object");
			}
			// Notify the main activity
			Intent intentEnergyModeSet = new Intent(ENERGY_MODE_SET);
			sendBroadcast(intentEnergyModeSet);
			// ACTION_GET_ENERGY_STATE: The service reads the energy settings
			// from the system and saves them in the IntialEnergySettings object
		} else if (intent.getAction().equals(ACTION_GET_ENERGY_STATE)) {
			InitialEnergySettings iniEnergySettings = InitialEnergySettings.getInstance();
			iniEnergySettings.setAirPlaneModeOn(isAirPlaneModeOn());
			iniEnergySettings.setAutoSyncOn(isAutoSyncOn());
			iniEnergySettings.setBluetoothOn(isBluetoothOn());
			iniEnergySettings.setWifiOn(isWifiOn());
			iniEnergySettings.setGPSOn(isGPSOn());
			iniEnergySettings.setCPUFrequency(getMaxCPUFrequency());
			iniEnergySettings.setGovernor(getCPUGovernor());
			// Notify the main activity
			Intent intentEnergyStateGet = new Intent(ENERGY_STATE_GET);
			sendBroadcast(intentEnergyStateGet);
			Log.d(getClass().getName(), "Initial energy settings saved: \n" + iniEnergySettings.toString());
		}
		stopSelf();// STOP itself
	}

	// Disable Bluetooth if enabled.
	private boolean disableBluetooth() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isEnabled())
				return mBluetoothAdapter.disable();
		}
		return true;
	}

	// Enable or disable wifi
	private boolean setWifiEnabled(boolean wifiEnabled) {
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		if (wifiManager != null)
			return wifiManager.setWifiEnabled(wifiEnabled);
		return false;
	}

	// Enable or disable system auto sync (updating apps, emails etc..)
	private void setAutoSynEnabled(boolean autoSynEnabled) {
		ContentResolver.setMasterSyncAutomatically(autoSynEnabled);
	}

	// Enable or disable airplane mode
	private void setAirplaneModeOn(boolean on) {
		// read the airplane mode setting
		boolean isEnabled = Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		if (isEnabled != on) {

			// toggle airplane mode
			Settings.System.putInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);

			// Post an intent to reload
			Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			intent.putExtra("state", !isEnabled);
			sendBroadcast(intent);
		}

	}

	// Set the maximum CPU frequency (THIS METHOD NEEDS A ROOT DEVICE TO WORK)
	private boolean setCpuMaxFrequency(int frequency) {

		try {
			// If the frequency isn't one of the available try to choose the
			// closest one.
			int freq = getClosestFreqToAvailable(frequency);
			if (freq <= 0)
				// problem comparing frequencies to those available
				return false;
			else {
				if (freq != getMaxCPUFrequency()) {
					Process process = Runtime.getRuntime().exec("su");
					DataOutputStream os = new DataOutputStream(process.getOutputStream());
					// For every CPU (core) write the max frequency in the file
					// scaling_max_freq
					for (int i = 0; i < getNumCPUs(); i++) {
						os.writeBytes("echo '" + freq + "' >> /sys/devices/system/cpu/cpu" + i
								+ "/cpufreq/scaling_max_freq\n");
					}
					os.writeBytes("exit\n");
					os.flush();
					os.close();
					process.waitFor();
				}
			}
		} catch (Exception e) {
			Log.e(e.getClass().getName(), e.getMessage(), e);
			return false;
		}
		return true;
	}

	// Return the closest available CPU frequency to the given one.
	private int getClosestFreqToAvailable(int frequency) {
		List<Integer> availableFrequencies = getAvailableFrequencies();
		if (availableFrequencies == null)
			return -1;
		else {
			if (!availableFrequencies.contains(frequency)) {
				int frq = 0;
				int diff = 0;
				int minDiff = Math.abs(availableFrequencies.get(0) - frequency);
				int closestFreq = availableFrequencies.get(0);
				for (int i = 1; i < availableFrequencies.size(); i++) {
					frq = availableFrequencies.get(i);
					diff = Math.abs(frq - frequency);
					if (diff < minDiff) {
						closestFreq = frq;
						minDiff = diff;
					}
				}
				return closestFreq;
			}
			return frequency;
		}

	}

	// Set the CPU governor (THIS METHOD NEEDS A ROOT DEVICE TO WORK)
	private boolean setCpuGovernor(String governor) {
		List<String> availableGovernors = getAvailableGovernors();
		if (availableGovernors != null) {
			if (availableGovernors.contains(governor)) {
				if (!governor.equals(getCPUGovernor())) {
					try {
						Process process = Runtime.getRuntime().exec("su");
						DataOutputStream os = new DataOutputStream(process.getOutputStream());
						// For every CPU (core) write the governor in the
						// scaling_governor file
						for (int i = 0; i < getNumCPUs(); i++) {
							os.writeBytes("echo '" + governor + "' >> /sys/devices/system/cpu/cpu" + i
									+ "/cpufreq/scaling_governor\n");
						}
						os.writeBytes("exit\n");
						os.flush();
						os.close();
						process.waitFor();
					} catch (Exception e) {
						Log.e(e.getClass().getName(), e.getMessage(), e);
						return false;
					}
				}
				return true;
			}
		}
		return false;

	}

	// Return the a list with the available governors. It reads them once form
	// the system and then cache them for future calls.
	// Returns null if the available governors couldn't be read
	private List<String> getAvailableGovernors() {
		if (availableGovernors != null)
			return availableGovernors;
		else {
			BufferedReader in = null;
			try {
				Process process;
				// Read list of available governors from file
				// /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors
				process = Runtime.getRuntime().exec(
						"cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors");
				in = new BufferedReader(new InputStreamReader(process.getInputStream()));
				ArrayList<String> governors = new ArrayList<String>();
				String line;
				while ((line = in.readLine()) != null) {
					String[] lines = line.split(" ");
					for (int i = 0; i < lines.length; i++) {
						String governor = lines[i].trim();
						if (governor.length() > 0)
							governors.add(governor);
					}
				}
				if (governors.size() <= 0)
					return null;
				else {
					availableGovernors = governors;
					return governors;
				}
			} catch (Exception e) {
				Log.e(e.getClass().getName(), e.getMessage(), e);
				return null;
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException e) {
					Log.e(e.getClass().getName(), e.getMessage(), e);
				}
			}
		}
	}

	// Returns the number of CPUs of the device or 0 if the number of CPUs
	// couldn't be read
	private int getNumCPUs() {
		int numCPUs = 0;
		BufferedReader in = null;
		try {
			/*
			 * To find out the number of CPUs it looks for folders inside
			 * /sys/devices/system/cpu/ which name is cpu plus one number from 0
			 * to 9. For example: /sys/devices/system/cpu/cpu0
			 * /sys/devices/system/cpu/cpu1 /sys/devices/system/cpu/cpu2
			 */
			Process process;
			process = Runtime.getRuntime().exec("ls /sys/devices/system/cpu");
			in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				String[] lines = line.split(" ");
				for (int i = 0; i < lines.length; i++) {
					String directory = lines[i].trim();
					if (Pattern.matches("cpu[0-9]", directory))
						numCPUs++;
				}
			}
		} catch (Exception e) {
			Log.e(e.getClass().getName(), e.getMessage(), e);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				Log.e(e.getClass().getName(), e.getMessage(), e);
			}
		}
		return numCPUs;
	}

	// Return the a list with the available frequencies. It reads them once form
	// the system and then cache them for future calls.
	// Returns null if the available frequencies couldn't be read
	private List<Integer> getAvailableFrequencies() {
		if (availableFrequencies != null)
			return availableFrequencies;
		else {
			BufferedReader in = null;
			try {
				Process process;
				// Reads the CPU available frequencies from
				// /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies
				process = Runtime.getRuntime().exec(
						"cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies");
				in = new BufferedReader(new InputStreamReader(process.getInputStream()));
				ArrayList<Integer> frequencies = new ArrayList<Integer>();
				String line;
				while ((line = in.readLine()) != null) {
					String[] lines = line.split(" ");
					for (int i = 0; i < lines.length; i++) {
						try {
							Integer freq = new Integer(lines[i].trim());
							frequencies.add(freq);
						} catch (NumberFormatException e) {
							Log.w(e.getClass().getName(), "Error parsing a value of CPU frequency");
						}
					}
				}
				if (frequencies.isEmpty())
					return null;
				else {
					availableFrequencies = frequencies;
					return frequencies;
				}
			} catch (Exception e) {
				Log.e(e.getClass().getName(), e.getMessage(), e);
				return null;
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException e) {
					Log.e(e.getClass().getName(), e.getMessage(), e);
				}
			}
		}

	}

	// if Bluetooth is enabled returns true, if not false
	private boolean isBluetoothOn() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isEnabled()) {
				return true;
			}
		}
		return false;
	}

	// if Wifi is enabled returns true, if not false
	private boolean isWifiOn() {
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		if (wifiManager != null) {
			if (wifiManager.isWifiEnabled())
				return true;
		}
		return false;

	}

	// if airplane mode is enabled returns true, if not false
	private boolean isAirPlaneModeOn() {
		// read the airplane mode setting
		return Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;

	}

	// if auto sync is enabled returns true, if not false
	private boolean isAutoSyncOn() {
		return ContentResolver.getMasterSyncAutomatically();
	}

	// if GPS is enabled returns true, if not false
	private boolean isGPSOn() {
		final LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
		return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	// Return the current maximum CPU frequency or -1 if something went wrong
	private int getMaxCPUFrequency() {
		BufferedReader in = null;
		try {
			Process process;
			process = Runtime.getRuntime().exec("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");
			in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			if ((line = in.readLine()) != null)
				return new Integer(line.trim());
			Log.e(getClass().getName(), "Error reading max requency, in.readLine() is null");

		} catch (Exception e) {
			Log.e(e.getClass().getName(), e.getMessage(), e);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				Log.e(e.getClass().getName(), e.getMessage(), e);
			}
		}
		return -1;
	}

	// Return the current CPU governor or null if something went wrong
	private String getCPUGovernor() {
		BufferedReader in = null;
		try {
			Process process;
			process = Runtime.getRuntime().exec("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
			in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			if ((line = in.readLine()) != null) {
				line = line.trim();
				if (getAvailableGovernors().contains(line))
					return line;
				Log.e(getClass().getName(),
						"The value reading from /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor is not a valid governor");
			} else
				Log.e(getClass().getName(), "Error reading scaling_governor, in.readLine() is null");

		} catch (Exception e) {
			Log.e(e.getClass().getName(), e.getMessage(), e);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				Log.e(e.getClass().getName(), e.getMessage(), e);
			}
		}
		return null;
	}
}
