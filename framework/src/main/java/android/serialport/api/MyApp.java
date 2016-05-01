/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package android.serialport.api;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;

import android.app.Application;
import android.content.SharedPreferences;
import android.serialport.api.SerialPort;
import android.serialport.api.SerialPortFinder;

public class MyApp extends Application{

	public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
	private SerialPort mSerialPort = null;

	public SerialPort getSerialPort(int databits, int stopbits, int parity) throws SecurityException, IOException, InvalidParameterException {
		if (mSerialPort == null) {
			/* Read serial port parameters */
			
			SharedPreferences sp = getSharedPreferences("com.example.serialtest_preferences", Application.MODE_PRIVATE);
			String path = sp.getString("DEVICE", "");
			int baudrate = Integer.decode(sp.getString("BAUDRATE", "-1"));

			/* Check parameters */
			if ( (path.length() == 0) || (baudrate == -1)) {
				throw new InvalidParameterException();
			}
		}
		return mSerialPort;
	}
}
