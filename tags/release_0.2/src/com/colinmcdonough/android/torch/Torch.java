/*
 * Copyright 2011 Colin McDonough
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.colinmcdonough.android.torch;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

/*
 * Torch is an LED flashlight.
 */
public class Torch extends Activity implements Eula.OnEulaAgreedTo {

  private static final String TAG = Torch.class.getSimpleName();

  private static final String WAKE_LOCK_TAG = "TORCH_WAKE_LOCK";

  private static final int COLOR_DARK = 0xCC000000;
  private static final int COLOR_LIGHT = 0xCCBFBFBF;
  private static final int COLOR_WHITE = 0xFFFFFFFF;

  private Camera mCamera;
  private boolean lightOn;
  private boolean previewOn;
  private boolean eulaAgreed;
  private View button;

  private WakeLock wakeLock;
  
  private static Torch torch;
  
  public Torch() {
    super();
    torch = this;
  }
  
  public static Torch getTorch() {
    return torch;
  }

  private void getCamera() {
    if (mCamera == null) {
      try {
        mCamera = Camera.open();
      } catch (RuntimeException e) {
      }
    }
  }

  /*
   * Called by the view (see main.xml)
   */
  public void toggleLight(View view) {
    toggleLight();
  }
  
  private void toggleLight() {
    if (lightOn) {
      turnLightOff();
    } else {
      turnLightOn();
    }
  }

  private void turnLightOn() {
    if (!eulaAgreed) {
      return;
    }
    if (mCamera == null) {
      Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG);
      // Use the screen as a flashlight (next best thing)
      button.setBackgroundColor(COLOR_WHITE);
      return;
    }
    lightOn = true;
    Parameters parameters = mCamera.getParameters();
    if (parameters == null) {
      // Use the screen as a flashlight (next best thing)
      button.setBackgroundColor(COLOR_WHITE);
      return;
    }
    List<String> flashModes = parameters.getSupportedFlashModes();
    // Check if camera flash exists
    if (flashModes == null) {
      // Use the screen as a flashlight (next best thing)
      button.setBackgroundColor(COLOR_WHITE);
      return;
    }
    String flashMode = parameters.getFlashMode();
    if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
      // Turn on the flash
      if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
        parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(parameters);
        button.setBackgroundColor(COLOR_LIGHT);
        startWakeLock();
      } else {
        Toast.makeText(this, "Flash mode (torch) not supported",
            Toast.LENGTH_LONG);
        // Use the screen as a flashlight (next best thing)
        button.setBackgroundColor(COLOR_WHITE);
        Log.e(TAG, "FLASH_MODE_TORCH not supported");
      }
    }
  }

  private void turnLightOff() {
    if (lightOn) {
      // set the background to dark
      button.setBackgroundColor(COLOR_DARK);
      lightOn = false;
      if (mCamera == null) {
        return;
      }
      Parameters parameters = mCamera.getParameters();
      if (parameters == null) {
        return;
      }
      List<String> flashModes = parameters.getSupportedFlashModes();
      String flashMode = parameters.getFlashMode();
      // Check if camera flash exists
      if (flashModes == null) {
        return;
      }
      if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {
        // Turn off the flash
        if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
          parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
          mCamera.setParameters(parameters);
          stopWakeLock();
        } else {
          Log.e(TAG, "FLASH_MODE_OFF not supported");
        }
      }
    }
  }

  private void startPreview() {
    if (!previewOn && mCamera != null) {
      mCamera.startPreview();
      previewOn = true;
    }
  }

  private void stopPreview() {
    if (previewOn && mCamera != null) {
      mCamera.stopPreview();
      previewOn = false;
    }
  }

  private void startWakeLock() {
    if (wakeLock == null) {
      PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
      wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
    }
    wakeLock.acquire();
  }

  private void stopWakeLock() {
    if (wakeLock != null) {
      wakeLock.release();
    }
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (Eula.show(this)) {
      eulaAgreed = true;
    }
    setContentView(R.layout.main);
    button = findViewById(R.id.button);
  }

  @Override
  public void onStart() {
    super.onStart();
    getCamera();
    startPreview();
  }

  @Override
  public void onResume() {
    super.onResume();
    turnLightOn();
  }

  @Override
  public void onStop() {
    super.onStop();
    if (mCamera != null) {
      stopPreview();
      mCamera.release();
      mCamera = null;
    };
    torch = null;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mCamera != null) {
      turnLightOff();
      stopPreview();
      mCamera.release();
    }
  }

  /** {@InheritDoc} **/
  @Override
  public void onEulaAgreedTo() {
    eulaAgreed = true;
    turnLightOn();
  }
  
  @Override
  public boolean onKeyLongPress(int keyCode, KeyEvent event) {
    // When the search button is long pressed, quit
    if (keyCode == KeyEvent.KEYCODE_SEARCH) {
      finish();
      return true;
    }
    return false;
  }
}