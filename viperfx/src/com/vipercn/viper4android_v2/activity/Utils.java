package com.vipercn.viper4android_v2.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.vipercn.viper4android_v2.R;
import com.vipercn.viper4android_v2.service.ViPER4AndroidService;

import com.stericson.RootTools.RootTools;
import com.stericson.RootShell.execution.Command;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

public class Utils {

    public class AudioEffectUtils {

        private AudioEffect.Descriptor[] mAudioEffectList;
        private boolean mHasViPER4AndroidEngine;
        private final int[] mV4AEngineVersion = new int[4];

        public AudioEffectUtils() {
            try {
                mAudioEffectList = AudioEffect.queryEffects();
            } catch (Exception e) {
                mAudioEffectList = null;
                mHasViPER4AndroidEngine = false;
                mV4AEngineVersion[0] = 0;
                mV4AEngineVersion[1] = 0;
                mV4AEngineVersion[2] = 0;
                mV4AEngineVersion[3] = 0;
                Log.e("ViPER4Android", "Failed to query audio effects");
                return;
            }
            if (mAudioEffectList == null) {
                mHasViPER4AndroidEngine = false;
                mV4AEngineVersion[0] = 0;
                mV4AEngineVersion[1] = 0;
                mV4AEngineVersion[2] = 0;
                mV4AEngineVersion[3] = 0;
                Log.e("ViPER4Android", "Failed to query audio effects");
                return;
            }

            AudioEffect.Descriptor mViper4AndroidEngine = null;
            Log.i("ViPER4Android", "Found " + mAudioEffectList.length + " effects");
            for (int i = 0; i < mAudioEffectList.length; i++) {
                if (mAudioEffectList[i] == null) continue;
                try {
                    AudioEffect.Descriptor aeEffect = mAudioEffectList[i];
                    Log.i("ViPER4Android", "[" + (i + 1) + "], " + aeEffect.name + ", "
                            + aeEffect.implementor);
                    if (aeEffect.uuid.equals(ViPER4AndroidService.ID_V4A_GENERAL_FX)) {
                        Log.i("ViPER4Android", "Perfect, found ViPER4Android engine at "
                                + (i + 1));
                        mViper4AndroidEngine = aeEffect;
                    }
                } catch (Exception e) {
                    Log.e("ViPER4Android",
                            "AudioEffect Descriptor error , msg = " + e.getMessage());
                }
            }

            if (mViper4AndroidEngine == null) {
                Log.i("ViPER4Android", "ViPER4Android engine not found");
                mHasViPER4AndroidEngine = false;
                mV4AEngineVersion[0] = 0;
                mV4AEngineVersion[1] = 0;
                mV4AEngineVersion[2] = 0;
                mV4AEngineVersion[3] = 0;
                return;
            }

            // Extract engine version
            try {
                String v4aVersionLine = mViper4AndroidEngine.name;
                if (v4aVersionLine.contains("[") && v4aVersionLine.contains("]")) {
                    if (v4aVersionLine.length() >= 23) {
                        // v4aVersionLine should be "ViPER4Android [A.B.C.D]"
                        v4aVersionLine = v4aVersionLine.substring(15);
                        while (v4aVersionLine.endsWith("]"))
                            v4aVersionLine = v4aVersionLine.substring(0, v4aVersionLine.length() - 1);
                        // v4aVersionLine should be "A.B.C.D"
                        String[] mVerBlocks = v4aVersionLine.split("\\.");
                        if (mVerBlocks.length == 4) {
                            mV4AEngineVersion[0] = Integer.parseInt(mVerBlocks[0]);
                            mV4AEngineVersion[1] = Integer.parseInt(mVerBlocks[1]);
                            mV4AEngineVersion[2] = Integer.parseInt(mVerBlocks[2]);
                            mV4AEngineVersion[3] = Integer.parseInt(mVerBlocks[3]);
                        }
                        Log.i("ViPER4Android", "The version of ViPER4Android engine is " +
                                mV4AEngineVersion[0] + "." +
                                mV4AEngineVersion[1] + "." +
                                mV4AEngineVersion[2] + "." +
                                mV4AEngineVersion[3]);
                        mHasViPER4AndroidEngine = true;
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e("ViPER4Android",
                        "ViPER4Android engine version exception: " + e.getMessage());
            }

            Log.e("ViPER4Android", "Cannot extract ViPER4Android engine version");
            mHasViPER4AndroidEngine = false;
            mV4AEngineVersion[0] = 0;
            mV4AEngineVersion[1] = 0;
            mV4AEngineVersion[2] = 0;
            mV4AEngineVersion[3] = 0;
        }

        public AudioEffect.Descriptor[] getAudioEffectList() {
            return mAudioEffectList;
        }

        public boolean isViPER4AndroidEngineFound() {
            return mHasViPER4AndroidEngine;
        }

        public int[] getViper4AndroidEngineVersion() {
            return mV4AEngineVersion;
        }
    }

    public static class CpuInfo {

        private boolean mCpuHasNEON;
        private boolean mCpuHasVFP;

        // Lets read /proc/cpuinfo in java
        private boolean readCpuInfo() {
            String mCPUInfoFile = "/proc/cpuinfo";
            FileReader cpuInfoReader = null;
            BufferedReader bufferReader = null;

            mCpuHasNEON = false;
            mCpuHasVFP = false;

            // Find "Features" line, extract neon and vfp
            try {
                cpuInfoReader = new FileReader(mCPUInfoFile);
                bufferReader = new BufferedReader(cpuInfoReader);
                while (true) {
                    String mLine = bufferReader.readLine();
                    if (mLine == null) {
                        break;
                    }
                    mLine = mLine.trim();
                    if (mLine.startsWith("Features")) {
                        Log.i("ViPER4Android", "CpuInfo[java] = <" + mLine + ">");
                        StringTokenizer stBlock = new StringTokenizer(mLine);
                        while (stBlock.hasMoreElements()) {
                            String mFeature = stBlock.nextToken();
                            if (mFeature != null) {
                                if (mFeature.equalsIgnoreCase("neon")) {
                                    mCpuHasNEON = true;
                                } else if (mFeature.equalsIgnoreCase("vfp")) {
                                    mCpuHasVFP = true;
                                }
                            }
                        }
                    }
                }
                bufferReader.close();
                cpuInfoReader.close();
                bufferReader = null;
                cpuInfoReader = null;

                Log.i("ViPER4Android", "cpuInfo[java] = NEON:" + mCpuHasNEON + ", VFP:"
                        + mCpuHasVFP);
                return !(!mCpuHasNEON && !mCpuHasVFP);
            } catch (IOException e) {
                try {
                    if (bufferReader != null) {
                        bufferReader.close();
                    }
                    if (cpuInfoReader != null) {
                        cpuInfoReader.close();
                    }
                    bufferReader = null;
                    cpuInfoReader = null;
                    return false;
                } catch (Exception ex) {
                    bufferReader = null;
                    cpuInfoReader = null;
                    return false;
                }
            }
        }

        // Lets read /proc/cpuinfo in jni
        private void readCPUInfoJni() {
            mCpuHasNEON = V4AJniInterface.IsCPUSupportNEON();
            mCpuHasVFP = V4AJniInterface.IsCPUSupportVFP();
        }

        // Buffered result
        public CpuInfo() {
            mCpuHasNEON = false;
            mCpuHasVFP = false;
            if (!readCpuInfo()) {
                readCPUInfoJni();
            }
        }

        public boolean hasNEON() {
            return mCpuHasNEON;
        }

        public boolean hasVFP() {
            return mCpuHasVFP;
        }
    }

    // Check if Busybox is installed & offer installation if not found
    public static boolean isBusyBoxInstalled(Context ctx) {
        boolean isBusyBoxAvailable = RootTools.isBusyboxAvailable();
        if (!isBusyBoxAvailable) {
            final Context ctxInstance = ctx;
            AlertDialog.Builder mBusyBox = new AlertDialog.Builder(ctxInstance);
            mBusyBox.setTitle("ViPER4Android");
            mBusyBox.setMessage(ctxInstance.getResources().getString(R.string.text_no_busybox));
            mBusyBox.setPositiveButton(ctxInstance.getResources().getString(R.string.text_ok),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        ctxInstance.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                                .parse("market://details?id=stericson.busybox")));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        ctxInstance.startActivity(new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                        "http://play.google.com/store/apps/details?id=stericson.busybox")));
                    }
                }
            });
            mBusyBox.setNegativeButton(ctxInstance.getResources().getString(R.string.text_cancel),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            mBusyBox.show();
            mBusyBox = null;
        }

        return isBusyBoxAvailable;
    }

    // Check for build.prop entries which can cause issues and offer update
    /*
     * The return value of this method is useless
     * Because this method will return before warning dialog dismiss
     */
    public static void proceedBuildProp(Context ctx) {
        final String LPA_DECODE = "lpa.decode";
        final String LPA_USE_STAGEFRIGHT = "lpa.use.stagefright";
        final String TUNNEL_DECODE = "tunnel.decode";

        final ArrayList<String> mBuildProp = new ArrayList<String>();
        final BuildProp mBuildPropUtil = new BuildProp();
        mBuildPropUtil.initializeBuildprop();

        if (mBuildPropUtil.propExists(LPA_DECODE)) {
            if (mBuildPropUtil.getProp(LPA_DECODE).equalsIgnoreCase("true")) {
            	Log.i("ViPER4Android", "[LPA] lpa.decode = true");
                mBuildProp.add(LPA_DECODE);
            } else Log.i("ViPER4Android", "[LPA] lpa.decode = false");
        }
        if (mBuildPropUtil.propExists(LPA_USE_STAGEFRIGHT)) {
            if (mBuildPropUtil.getProp(LPA_USE_STAGEFRIGHT).equalsIgnoreCase("true")) {
            	Log.i("ViPER4Android", "[LPA] lpa.use.stagefright = true");
                mBuildProp.add(LPA_USE_STAGEFRIGHT);
            } else Log.i("ViPER4Android", "[LPA] lpa.use.stagefright = false");
        }
        if (mBuildPropUtil.propExists(TUNNEL_DECODE)) {
            if (mBuildPropUtil.getProp(TUNNEL_DECODE).equalsIgnoreCase("true")) {
            	Log.i("ViPER4Android", "[LPA] tunnel.decode = true");
                mBuildProp.add(TUNNEL_DECODE);
            } else Log.i("ViPER4Android", "[LPA] tunnel.decode = false");
        }

        if (!mBuildProp.isEmpty()) {
            AlertDialog.Builder mModifyWarn = new AlertDialog.Builder(ctx);
            mModifyWarn.setTitle("ViPER4Android");
            mModifyWarn.setMessage(ctx.getResources().getString(R.string.text_modifybuildprop));
            mModifyWarn.setPositiveButton(ctx.getResources().getString(R.string.text_ok),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                	for (String buildProp : mBuildProp) {
                		mBuildPropUtil.setProp(buildProp, "false");
                	}
                	mBuildPropUtil.commitBuildprop(StaticEnvironment.getExternalStoragePath());
                	mBuildPropUtil.initializeBuildprop();
                }
            });
            mModifyWarn.setNegativeButton(ctx.getResources().getString(R.string.text_cancel),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                	Log.i("ViPER4Android", "[LPA] User canceled");
                    dialog.dismiss();
                }
            });
            mModifyWarn.show();
            mModifyWarn = null;
        } else {
        	Log.i("ViPER4Android", "LPA feature not enabled");
        }
    }

    // Get a file length
    private static long getFileLength(String mFileName) {
        if (!(new File(mFileName).isFile())) return 0;
        return new File(mFileName).length();
    }

    // Read file list from path
    public static void getFileNameList(File path, String fileExt, ArrayList<String> fileList) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (null == files) return;
            for (File file : files) getFileNameList(file, fileExt, fileList);
        } else {
            String filePath = path.getAbsolutePath();
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            if (fileName.toLowerCase(Locale.US).endsWith(fileExt))
                fileList.add(fileName);
        }
    }

    // Get profile name from a file
    private static String getProfileName(String mProfileFileName) {
        try {
            FileInputStream fisInput = new FileInputStream(mProfileFileName);
            InputStreamReader isrInput = new InputStreamReader(fisInput, "UTF-8");
            BufferedReader bufferInput = new BufferedReader(isrInput);
            String mProfileName = "";
            while (true) {
                String mLine = bufferInput.readLine();
                if (mLine == null) break;
                if (mLine.startsWith("#")) continue;

                String[] mChunks = mLine.split("=");
                if (mChunks.length != 2) continue;
                if (mChunks[0].trim().equalsIgnoreCase("profile_name")) {
                    mProfileName = mChunks[1];
                    break;
                }
            }
            bufferInput.close();
            isrInput.close();
            fisInput.close();
            bufferInput = null;
            isrInput = null;
            fisInput = null;
            return mProfileName;
        } catch (Exception e) {
            return "";
        }
    }

    // Get profile name list
    public static ArrayList<String> getProfileList(String mProfileDir) {
        try {
            File fProfileDirHandle = new File(mProfileDir);
            ArrayList<String> profileList = new ArrayList<String>();
            getFileNameList(fProfileDirHandle, ".prf", profileList);

            ArrayList<String> mProfileNameList = new ArrayList<String>();
            for (String mProfileList : profileList) {
                String mFileName = mProfileDir + mProfileList;
                String mName = getProfileName(mFileName);
                mProfileNameList.add(mName.trim());
            }
            profileList = null;

            return mProfileNameList;
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    // Load profile from file
    public static boolean loadProfileV1(String mProfileName, String mProfileDir,
            String mPreferenceName, Context ctx) {
        try {
            File fProfileDirHandle = new File(mProfileDir);
            ArrayList<String> profileFileList = new ArrayList<String>();
            getFileNameList(fProfileDirHandle, ".prf", profileFileList);
            String mProfileFileName = "";
            for (String mProfileFileList : profileFileList) {
                String mFileName = mProfileDir + mProfileFileList;
                String mName = getProfileName(mFileName);
                if (mProfileName.trim().equalsIgnoreCase(mName.trim())) {
                    mProfileFileName = mFileName;
                    break;
                }
            }
            if (mProfileFileName.equals("")) return false;

            SharedPreferences preferences = ctx.getSharedPreferences(mPreferenceName,
                    Context.MODE_PRIVATE);
            if (preferences != null) {
                FileInputStream fisInput = new FileInputStream(mProfileFileName);
                InputStreamReader isrInput = new InputStreamReader(fisInput, "UTF-8");
                BufferedReader bufferInput = new BufferedReader(isrInput);
                Editor e = preferences.edit();
                while (true) {
                    String mLine = bufferInput.readLine();
                    if (mLine == null) break;
                    if (mLine.startsWith("#")) continue;

                    String[] mChunks = mLine.split("=");
                    if (mChunks.length != 3) continue;
                    if (mChunks[1].trim().equalsIgnoreCase("boolean")) {
                        String mParameter = mChunks[0];
                        boolean mValue = Boolean.valueOf(mChunks[2]);
                        e.putBoolean(mParameter, mValue);
                    } else if (mChunks[1].trim().equalsIgnoreCase("string")) {
                        String mParameter = mChunks[0];
                        String mValue = mChunks[2];
                        e.putString(mParameter, mValue);
                    } else {
                    }
                }
                e.commit();
                bufferInput.close();
                isrInput.close();
                fisInput.close();
                bufferInput = null;
                isrInput = null;
                fisInput = null;
                return true;
            } else
                return false;
        } catch (Exception e) {
            Log.i("ViPER4Android", "loadProfile Error: " + e.getMessage());
            return false;
        }
    }

    // Check file
    private static boolean checkFileContainsKeyLower(String szFilePathName, String szKey, String szEncoding)
    {
    	szKey = szKey.trim();

		FileInputStream fisInput = null;
		InputStreamReader isrInput = null;
		BufferedReader bufferInput = null;
		try {
			boolean bFoundKey = false;
			fisInput = new FileInputStream(szFilePathName);
			isrInput = new InputStreamReader(fisInput, szEncoding);
			bufferInput = new BufferedReader(isrInput);
			while (true) {
				String currLine = bufferInput.readLine();
				if (currLine == null) break;
				if (currLine.trim().startsWith("#")) continue;
				if (currLine.toLowerCase(Locale.US).contains(szKey)) {
					bFoundKey = true;
					break;
				}
			}
			bufferInput.close();
			isrInput.close();
			fisInput.close();
			fisInput = null;
			isrInput = null;
			bufferInput = null;
			return bFoundKey;
		} catch (FileNotFoundException e) {
			fisInput = null;
			isrInput = null;
			bufferInput = null;
			return false;
		} catch (UnsupportedEncodingException e) {
			fisInput = null;
			isrInput = null;
			bufferInput = null;
			return false;
		} catch (IOException e) {
			fisInput = null;
			isrInput = null;
			bufferInput = null;
			return false;
		}
    }

    /*
     * Return value:
     * 0: Success
     * 5: Input File Error
     * 3: I/O Error
     * 4: Unsupported file format
     */

    // Modify audio_effects.conf
    private static int modifyFXConfig(String mInputFile, String mOutputFile) {
        Log.i("ViPER4Android", "Editing audio configuration, input = " + mInputFile
                + ", output = " + mOutputFile);
        try {
            long inputFileLength = getFileLength(mInputFile);
            if (inputFileLength < 32) {
            	// We need at least 32-bytes for basic structure
            	return 5;
            }
            new File(mOutputFile).delete();

            // Create reading and writing stuff
            FileInputStream fisInput = new FileInputStream(mInputFile);
            FileOutputStream fosOutput = new FileOutputStream(mOutputFile);
            InputStreamReader isrInput = new InputStreamReader(fisInput, "ASCII");
            OutputStreamWriter oswOutput = new OutputStreamWriter(fosOutput, "ASCII");
            BufferedReader bufferInput = new BufferedReader(isrInput);
            BufferedWriter bufferOutput = new BufferedWriter(oswOutput);

            // Check whether the file has already modified
            boolean configModified = false;
            bufferInput.mark((int) inputFileLength);
            do {
                String mLine = bufferInput.readLine();
                if (mLine == null) break;
                if (mLine.trim().startsWith("#")) continue;
                /* This is v4a effect uuid */
                if (mLine.toLowerCase(Locale.US)
                        .contains("41d3c987-e6cf-11e3-a88a-11aba5d5c51b")) {
                    Log.i("ViPER4Android", "Source file has been modified, line = " + mLine);
                    configModified = true;
                    break;
                }
            } while (true);

            boolean libraryAppend = false;
            boolean effectAppend = false;
            if (configModified) {
                // Already modified, just copy
                bufferInput.reset();
                do {
                    String mLine = bufferInput.readLine();
                    if (mLine == null) break;
                    bufferOutput.write(mLine + "\n");
                } while (true);
                bufferOutput.flush();

                bufferInput.close();
                isrInput.close();
                fisInput.close();
                bufferOutput.close();
                oswOutput.close();
                fosOutput.close();

                return 0;
            } else {
                // Append v4a library and effect to configuration
                bufferInput.reset();
                do {
                    String mLine = bufferInput.readLine();
                    if (mLine == null) break;
                    if (mLine.trim().startsWith("#")) {
                    	bufferOutput.write(mLine + "\n");
                    	continue;
                    }
                    if (mLine.trim().equalsIgnoreCase("libraries {") && !libraryAppend) {
                        // Append library
                        bufferOutput.write(mLine + "\n");
                        bufferOutput.write("  v4a_fx {\n");
                        bufferOutput.write("    path /system/lib/soundfx/libv4a_fx_ics.so\n");
                        bufferOutput.write("  }\n");
                        libraryAppend = true;
                    } else if (mLine.trim().equalsIgnoreCase("effects {") && !effectAppend) {
                        // Append effect
                        bufferOutput.write(mLine + "\n");
                        bufferOutput.write("  v4a_standard_fx {\n");
                        bufferOutput.write("    library v4a_fx\n");
                        bufferOutput.write("    uuid 41d3c987-e6cf-11e3-a88a-11aba5d5c51b\n");
                        bufferOutput.write("  }\n");
                        effectAppend = true;
                    } else bufferOutput.write(mLine + "\n");
                } while (true);
                bufferOutput.flush();

                bufferInput.close();
                isrInput.close();
                fisInput.close();
                bufferOutput.close();
                oswOutput.close();
                fosOutput.close();

                if (libraryAppend && effectAppend) return 0;
                return 4;
            }
        } catch (Exception e) {
            Log.i("ViPER4Android", "Error: " + e.getMessage());
            return 3;
        }
    }

    // Get application data path
    public static String getBasePath(Context ctx) {
        Context mContext = ctx.getApplicationContext();
        String mBasePath = "";
        if (mContext != null) {
            // No try catch the mContext != null will prevent a possible NPE here
            if (mContext.getFilesDir().exists()) {
                mBasePath = mContext.getFilesDir().getAbsolutePath();
            } else if (!mContext.getFilesDir().mkdirs()) {
                mBasePath = "";
            }
        } else {
            mBasePath = "";
        }
        return mBasePath;
    }

    // Copy assets to local
    public static boolean copyAssetsToLocal(Context ctx, String mSourceName, String mDestinationName) {
        String mBasePath = getBasePath(ctx);
        if (mBasePath.equals("")) return false;
        mDestinationName = mBasePath + "/" + mDestinationName;

        InputStream myInput;
        OutputStream myOutput;
        String outFileName = mDestinationName;
        try {
            File hfOutput = new File(mDestinationName);
            if (hfOutput.exists()) hfOutput.delete();
            myOutput = new FileOutputStream(outFileName);
            myInput = ctx.getAssets().open(mSourceName);
            byte[] buffer = new byte[4096]; /* 4K page size */
            int length;
            while ((length = myInput.read(buffer)) > 0)
                myOutput.write(buffer, 0, length);
            myOutput.flush();
            myInput.close();
            myOutput.close();
            buffer = null;
            myInput = null;
            myOutput = null;
        } catch (Exception e) {
            Log.i("ViPER4Android", "CopyAssetsToLocal() failed, msg = " + e.getMessage());
            return false;
        }

        return true;
    }

    // Uninstall ViPER4Android FX driver
    public static void uninstallDrv_FX() {
      return;
    }

    /*
     * Driver installation return value:
     * 0: Success
     * 1: Acquire root failed
     * 2: External storage not mounted
     * 3: I/O error
     * 4: Unsupported audio_config.conf file format
     * 5: Busybox not found
     * 6: Unknow error
     */

    // Install ViPER4Android FX driver through roottools
    private static int installDrv_FX_RootTools(Context ctx, String mDriverName) {
        return 0;
        }

    // Install ViPER4Android FX driver
    public static int installDrv_FX(Context ctx, String mDriverName) {
    	return 0;
    }

    /**
     * Restart the activity smoothly
     *
     * @param activity
     */
    public static void restartActivity(final Activity activity) {
        if (activity == null) return;
        final int enter_anim = android.R.anim.fade_in;
        final int exit_anim = android.R.anim.fade_out;
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.finish();
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.startActivity(activity.getIntent());
    }
}
