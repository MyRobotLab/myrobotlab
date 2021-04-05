package com.oculusvr.capi;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public interface OvrLibrary extends Library {
  public static final String OVR_KEY_USER = "User";
  public static final String OVR_KEY_NAME = "Name";
  public static final String OVR_KEY_GENDER = "Gender";
  public static final String OVR_KEY_PLAYER_HEIGHT = "PlayerHeight";
  public static final String OVR_KEY_EYE_HEIGHT = "EyeHeight";
  public static final String OVR_KEY_IPD = "IPD";
  public static final String OVR_KEY_NECK_TO_EYE_DISTANCE = "NeckEyeDistance";
  public static final String OVR_KEY_EYE_RELIEF_DIAL = "EyeReliefDial";
  public static final String OVR_KEY_EYE_TO_NOSE_DISTANCE = "EyeToNoseDist";
  public static final String OVR_KEY_MAX_EYE_TO_PLATE_DISTANCE = "MaxEyeToPlateDist";
  public static final String OVR_KEY_EYE_CUP = "EyeCup";
  public static final String OVR_KEY_CUSTOM_EYE_RENDER = "CustomEyeRender";
  public static final String OVR_KEY_CAMERA_POSITION_1 = "CenteredFromWorld";
  public static final String OVR_KEY_CAMERA_POSITION_2 = "CenteredFromWorld2";
  public static final String OVR_KEY_CAMERA_POSITION = OVR_KEY_CAMERA_POSITION_2;

  public static final String OVR_DEFAULT_GENDER = "Unknown";
  public static final float OVR_DEFAULT_NECK_TO_EYE_VERTICAL = 0.075f;
  public static final float OVR_DEFAULT_PLAYER_HEIGHT = 1.778f;
  public static final float OVR_DEFAULT_IPD = 0.064f;
  public static final float OVR_DEFAULT_NECK_TO_EYE_HORIZONTAL = 0.0805f;
  public static final float OVR_DEFAULT_EYE_HEIGHT = 1.675f;
  public static final int OVR_DEFAULT_EYE_RELIEF_DIAL = 3;
  public static final String OVR_PERF_HUD_MODE = "PerfHudMode";

  public static final String OVR_LAYER_HUD_MODE = "LayerHudMode"; // allowed
                                                                  // values are
                                                                  // defined in
                                                                  // enum
                                                                  // ovrLayerHudMode
  public static final String OVR_LAYER_HUD_CURRENT_LAYER = "LayerHudCurrentLayer"; // The
                                                                                   // layer
                                                                                   // to
                                                                                   // show
  public static final String OVR_LAYER_HUD_SHOW_ALL_LAYERS = "LayerHudShowAll"; // Hide
                                                                                // other
                                                                                // layers
                                                                                // when
                                                                                // the
                                                                                // hud
                                                                                // is
                                                                                // enabled

  public static final String OVR_DEBUG_HUD_STEREO_MODE = "DebugHudStereoMode";
  public static final String OVR_DEBUG_HUD_STEREO_GUIDE_INFO_ENABLE = "DebugHudStereoGuideInfoEnable";
  public static final String OVR_DEBUG_HUD_STEREO_GUIDE_SIZE = "DebugHudStereoGuideSize2f";
  public static final String OVR_DEBUG_HUD_STEREO_GUIDE_POSITION = "DebugHudStereoGuidePosition3f";
  public static final String OVR_DEBUG_HUD_STEREO_GUIDE_YAWPITCHROLL = "DebugHudStereoGuideYawPitchRoll3f";
  public static final String OVR_DEBUG_HUD_STEREO_GUIDE_COLOR = "DebugHudStereoGuideColor4f";

  public static final int OVR_PRODUCT_VERSION = 1;
  public static final int OVR_MAJOR_VERSION = 1;
  public static final int OVR_MINOR_VERSION = 3;
  public static final int OVR_PATCH_VERSION = 0;
  public static final int OVR_BUILD_NUMBER = 0;

  public static final String BIT_DEPTH = System.getProperty("sun.arch.data.model");
  public static final String LIBRARY_NAME = String.format("LibOVRRT%s_%d.dll", BIT_DEPTH, OVR_PRODUCT_VERSION, OVR_MAJOR_VERSION);
  public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(LIBRARY_NAME);
  public static final OvrLibrary INSTANCE = (OvrLibrary) Native.loadLibrary(LIBRARY_NAME, OvrLibrary.class);

  public static interface ovrInitFlags {

    /// When a debug library is requested, a slower debugging version of the
    /// library will
    /// run which can be used to help solve problems in the library and debug
    /// application code.
    public static final int ovrInit_Debug = 0x00000001;

    /// When a version is requested, the LibOVR runtime respects the
    /// RequestedMinorVersion
    /// field and verifies that the RequestedMinorVersion is supported. Normally
    /// when you
    /// specify this flag you simply use OVR_MINOR_VERSION for
    /// ovrInitParams::RequestedMinorVersion,
    /// though you could use a lower version than OVR_MINOR_VERSION to specify
    /// previous
    /// version behavior.
    public static final int ovrInit_RequestVersion = 0x00000004;

    // These bits are writable by user code.
    public static final int ovrinit_WritableBits = 0x00ffffff;
  }

  public static interface ovrLogLevel {

    public static final int ovrLogLevel_Debug = 0; /// < Debug-level log event.
    public static final int ovrLogLevel_Info = 1; /// < Info-level log event.
    public static final int ovrLogLevel_Error = 2; /// < Error-level log event.

  }

  public static interface ovrSuccessType {
    public static final int ovrSuccess = 0;
    public static final int ovrSuccess_NotVisible = 1000;
    public static final int ovrSuccess_HMDFirmwareMismatch = 4100;
    public static final int ovrSuccess_TrackerFirmwareMismatch = 4101;
  }

  public static interface ovrErrorType {
    public static final int ovrError_MemoryAllocationFailure = -1000;
    public static final int ovrError_SocketCreationFailure = -1001;
    public static final int ovrError_InvalidHmd = -1002;
    public static final int ovrError_Timeout = -1003;
    public static final int ovrError_NotInitialized = -1004;
    public static final int ovrError_InvalidParameter = -1005;
    public static final int ovrError_ServiceError = -1006;
    public static final int ovrError_NoHmd = -1007;
    public static final int ovrError_InvalidHeadsetOrientation = -1011; /// <
                                                                        /// The
                                                                        /// headset
                                                                        /// was
                                                                        /// in
                                                                        /// an
                                                                        /// invalid
                                                                        /// orientation
                                                                        /// for
                                                                        /// the
                                                                        /// requested
                                                                        /// operation
                                                                        /// (e.g.
                                                                        /// vertically
                                                                        /// oriented
                                                                        /// during
                                                                        /// ovr_RecenterPose).

    public static final int ovrError_AudioReservedBegin = -2000;
    public static final int ovrError_AudioReservedEnd = -2999;

    public static final int ovrError_Initialize = -3000;
    public static final int ovrError_LibLoad = -3001;
    public static final int ovrError_LibVersion = -3002;
    public static final int ovrError_ServiceConnection = -3003;
    public static final int ovrError_ServiceVersion = -3004;
    public static final int ovrError_IncompatibleOS = -3005;
    public static final int ovrError_DisplayInit = -3006;
    public static final int ovrError_ServerStart = -3007;
    public static final int ovrError_Reinitialization = -3008;
    public static final int ovrError_MismatchedAdapters = -3009;
    public static final int ovrError_LeakingResources = -3010;
    public static final int ovrError_ClientVersion = -3011;

    public static final int ovrError_InvalidBundleAdjustment = -4000;
    public static final int ovrError_USBBandwidth = -4001;
    public static final int ovrError_USBEnumeratedSpeed = -4002;
    public static final int ovrError_ImageSensorCommError = -4003;
    public static final int ovrError_GeneralTrackerFailure = -4004;
    public static final int ovrError_ExcessiveFrameTruncation = -4005;
    public static final int ovrError_ExcessiveFrameSkipping = -4006;
    public static final int ovrError_SyncDisconnected = -4007;
    public static final int ovrError_TrackerMemoryReadFailure = -4008;
    public static final int ovrError_TrackerMemoryWriteFailure = -4009;
    public static final int ovrError_TrackerFrameTimeout = -4010;
    public static final int ovrError_TrackerTruncatedFrame = -4011;

    public static final int ovrError_HMDFirmwareMismatch = -4100;
    public static final int ovrError_TrackerFirmwareMismatch = -4101;
    public static final int ovrError_BootloaderDeviceDetected = -4102;
    public static final int ovrError_TrackerCalibrationError = -4103;
    public static final int ovrError_Incomplete = -5000;
    public static final int ovrError_Abandoned = -5001;
    public static final int ovrError_DisplayLost = -6000;
  }

  public static interface ovrHmdType {
    public static final int ovrHmd_None = 0;
    public static final int ovrHmd_DK1 = 3;
    public static final int ovrHmd_DKHD = 4;
    public static final int ovrHmd_DK2 = 6;
    public static final int ovrHmd_CB = 8;
    public static final int ovrHmd_Other = 9;
    public static final int ovrHmd_E3_2015 = 10;
    public static final int ovrHmd_ES06 = 11;
    public static final int ovrHmd_ES09 = 12;
    public static final int ovrHmd_ES11 = 13;
    public static final int ovrHmd_CV1 = 14;

  };

  public static interface ovrHmdCaps {
    public static final int ovrHmdCap_DebugDevice = 0x0001;
    public static final int ovrHmdCap_Writable_Mask = 0x0000;
    public static final int ovrHmdCap_Service_Mask = 0x0000;
  };

  public static interface ovrTrackingCaps {
    public static final int ovrTrackingCap_Orientation = 0x0010;
    public static final int ovrTrackingCap_MagYawCorrection = 0x0020;
    public static final int ovrTrackingCap_Position = 0x0040;
  };

  public static interface ovrDistortionCaps {
    public static final int ovrDistortionCap_Chromatic = 0x01;
    public static final int ovrDistortionCap_TimeWarp = 0x02;
    public static final int ovrDistortionCap_Vignette = 0x08;
    public static final int ovrDistortionCap_NoRestore = 0x10;
    public static final int ovrDistortionCap_FlipInput = 0x20;
    public static final int ovrDistortionCap_SRGB = 0x40;
    public static final int ovrDistortionCap_Overdrive = 0x80;
    public static final int ovrDistortionCap_HqDistortion = 0x100;
    public static final int ovrDistortionCap_LinuxDevFullscreen = 0x200;
    public static final int ovrDistortionCap_ComputeShader = 0x400;
    public static final int ovrDistortionCap_ProfileNoTimewarpSpinWaits = 0x10000;
  };

  public static interface ovrEyeType {
    public static final int ovrEye_Left = 0;
    public static final int ovrEye_Right = 1;
    public static final int ovrEye_Count = 2;
  };

  public static interface ovrStatusBits {
    public static final int ovrStatus_OrientationTracked = 0x0001;
    public static final int ovrStatus_PositionTracked = 0x0002;
    public static final int ovrStatus_HmdConnected = 0x0080;
  };

  public static interface ovrRenderAPIType {
    public static final int ovrRenderAPI_None = 0;
    public static final int ovrRenderAPI_OpenGL = 1;
    public static final int ovrRenderAPI_Android_GLES = 2;
    public static final int ovrRenderAPI_D3D11 = 5;
  };

  public static interface ovrLayerType {
    public static final int ovrLayerType_Disabled = 0;
    public static final int ovrLayerType_EyeFov = 1;
    public static final int ovrLayerType_Quad = 3;
    public static final int ovrLayerType_EyeMatrix = 5;
  };

  public static interface ovrLayerFlags {
    public static final int ovrLayerFlag_HighQuality = 0x01;
    public static final int ovrLayerFlag_TextureOriginAtBottomLeft = 0x02;

    /// Mark this surface as "headlocked", which means it is specified
    /// relative to the HMD and moves with it, rather than being specified
    /// relative to sensor/torso space and remaining still while the head moves.
    /// ovrLayerType_QuadHeadLocked is now ovrLayerType_Quad plus this flag.
    /// However the flag can be applied to any layer type except
    /// ovrLayerType_Direct
    /// to achieve a similar effect.
    public static final int ovrLayerFlag_HeadLocked = 0x04;

  };

  public static interface ovrProjectionModifier {
    public static final int ovrProjection_None = 0x00;
    public static final int ovrProjection_LeftHanded = 0x01;
    public static final int ovrProjection_FarLessThanNear = 0x02;
    public static final int ovrProjection_FarClipAtInfinity = 0x04;
    public static final int ovrProjection_ClipRangeOpenGL = 0x08;
  };

  /// Describes button input types.
  /// Button inputs are combined; that is they will be reported as pressed if
  /// they are
  /// pressed on either one of the two devices.
  /// The ovrButton_Up/Down/Left/Right map to both XBox D-Pad and directional
  /// buttons.
  /// The ovrButton_Enter and ovrButton_Return map to Start and Back controller
  /// buttons, respectively.
  public static interface ovrButton {
    public static final int ovrButton_A = 0x00000001;
    public static final int ovrButton_B = 0x00000002;
    public static final int ovrButton_RThumb = 0x00000004;
    public static final int ovrButton_RShoulder = 0x00000008;
    public static final int ovrButton_X = 0x00000100;
    public static final int ovrButton_Y = 0x00000200;
    public static final int ovrButton_LThumb = 0x00000400;
    public static final int ovrButton_LShoulder = 0x00000800;

    // Navigation through DPad.
    public static final int ovrButton_Up = 0x00010000;
    public static final int ovrButton_Down = 0x00020000;
    public static final int ovrButton_Left = 0x00040000;
    public static final int ovrButton_Right = 0x00080000;
    public static final int ovrButton_Enter = 0x00100000; // Start on XBox
                                                          // controller.
    public static final int ovrButton_Back = 0x00200000; // Back on Xbox
                                                         // controller.

    public static final int ovrButton_Private = 0x00400000 | 0x00800000 | 0x01000000;
  };

  /// The type of texture resource.
  ///
  /// @see ovrTextureSwapChainDesc
  ///
  public static interface ovrTextureType {

    public static final int ovrTexture_2D = 0; /// < 2D textures.
    public static final int ovrTexture_2D_External = 1; /// < External 2D
                                                        /// texture. Not used on
                                                        /// PC
    public static final int ovrTexture_Cube = 2; /// < Cube maps. Not currently
                                                 /// supported on PC.
    public static final int ovrTexture_Count = 3;
  };

  /// The format of a texture.
  ///
  /// \see ovrTextureSwapChainDesc
  ///
  public static interface ovrTextureFormat {

    public static final int OVR_FORMAT_UNKNOWN = 0;
    public static final int OVR_FORMAT_B5G6R5_UNORM = 1; /// < Not currently
                                                         /// supported on PC.
                                                         /// Would require a
                                                         /// DirectX 11.1
                                                         /// device.
    public static final int OVR_FORMAT_B5G5R5A1_UNORM = 2; /// < Not currently
                                                           /// supported on PC.
                                                           /// Would require a
                                                           /// DirectX 11.1
                                                           /// device.
    public static final int OVR_FORMAT_B4G4R4A4_UNORM = 3; /// < Not currently
                                                           /// supported on PC.
                                                           /// Would require a
                                                           /// DirectX 11.1
                                                           /// device.
    public static final int OVR_FORMAT_R8G8B8A8_UNORM = 4;
    public static final int OVR_FORMAT_R8G8B8A8_UNORM_SRGB = 5;
    public static final int OVR_FORMAT_B8G8R8A8_UNORM = 6;
    public static final int OVR_FORMAT_B8G8R8A8_UNORM_SRGB = 7; /// < Not
                                                                /// supported
                                                                /// for OpenGL
                                                                /// applications
    public static final int OVR_FORMAT_B8G8R8X8_UNORM = 8; /// < Not supported
                                                           /// for OpenGL
                                                           /// applications
    public static final int OVR_FORMAT_B8G8R8X8_UNORM_SRGB = 9; /// < Not
                                                                /// supported
                                                                /// for OpenGL
                                                                /// applications
    public static final int OVR_FORMAT_R16G16B16A16_FLOAT = 10;
    public static final int OVR_FORMAT_D16_UNORM = 11;
    public static final int OVR_FORMAT_D24_UNORM_S8_UINT = 12;
    public static final int OVR_FORMAT_D32_FLOAT = 13;
    public static final int OVR_FORMAT_D32_FLOAT_S8X24_UINT = 13;

  };

  /// Specifies sensor flags.
  ///
  /// /see ovrTrackerPose
  ///
  public static interface ovrTrackerFlags {

    public static final int ovrTracker_Connected = 0x0020; /// < The sensor is
                                                           /// present, else the
                                                           /// sensor is absent
                                                           /// or offline.
    public static final int ovrTracker_PoseTracked = 0x0004; /// < The sensor
                                                             /// has a valid
                                                             /// pose, else the
                                                             /// pose is
                                                             /// unavailable.
                                                             /// This will only
                                                             /// be set if
                                                             /// ovrTracker_Connected
                                                             /// is set.

  }

  /// Specifies the maximum number of layers supported by ovr_SubmitFrame.
  ///
  /// /see ovr_SubmitFrame
  ///
  public static final int ovrMaxLayerCount = 16;

  // -----------------------------------------------------------------------------------
  // ***** API Interfaces

  /// Initializes LibOVR
  ///
  /// Initialize LibOVR for application usage. This includes finding and loading
  /// the LibOVRRT
  /// shared library. No LibOVR API functions, other than ovr_GetLastErrorInfo
  /// and ovr_Detect, can
  /// be called unless ovr_Initialize succeeds. A successful call to
  /// ovr_Initialize must be eventually
  /// followed by a call to ovr_Shutdown. ovr_Initialize calls are idempotent.
  /// Calling ovr_Initialize twice does not require two matching calls to
  /// ovr_Shutdown.
  /// If already initialized, the return value is ovr_Success.
  ///
  /// LibOVRRT shared library search order:
  /// -# Current working directory (often the same as the application
  /// directory).
  /// -# Module directory (usually the same as the application directory,
  /// but not if the module is a separate shared library).
  /// -# Application directory
  /// -# Development directory (only if OVR_ENABLE_DEVELOPER_SEARCH is enabled,
  /// which is off by default).
  /// -# Standard OS shared library search location(s) (OS-specific).
  ///
  /// \param params Specifies custom initialization options. May be NULL to
  /// indicate default options when
  /// using the CAPI shim. If you are directly calling the LibOVRRT version of
  /// ovr_Initialize
  // in the LibOVRRT DLL then this must be valid and include
  /// ovrInit_RequestVersion.
  /// \return Returns an ovrResult indicating success or failure. In the case of
  /// failure, use
  /// ovr_GetLastErrorInfo to get more information. Example failed results
  /// include:
  /// - ovrError_Initialize: Generic initialization error.
  /// - ovrError_LibLoad: Couldn't load LibOVRRT.
  /// - ovrError_LibVersion: LibOVRRT version incompatibility.
  /// - ovrError_ServiceConnection: Couldn't connect to the OVR Service.
  /// - ovrError_ServiceVersion: OVR Service version incompatibility.
  /// - ovrError_IncompatibleOS: The operating system version is incompatible.
  /// - ovrError_DisplayInit: Unable to initialize the HMD display.
  /// - ovrError_ServerStart: Unable to start the server. Is it already running?
  /// - ovrError_Reinitialization: Attempted to re-initialize with a different
  /// version.
  ///
  /// <b>Example code</b>
  /// \code{.cpp}
  /// ovrInitParams initParams = { ovrInit_RequestVersion, OVR_MINOR_VERSION,
  /// NULL, 0, 0 };
  /// ovrResult result = ovr_Initialize(&initParams);
  /// if(OVR_FAILURE(result)) {
  /// ovrErrorInfo errorInfo;
  /// ovr_GetLastErrorInfo(&errorInfo);
  /// DebugLog("ovr_Initialize failed: %s", errorInfo.ErrorString);
  /// return false;
  /// }
  /// [...]
  /// \endcode
  ///
  /// \see ovr_Shutdown
  ///
  int ovr_Initialize(InitParams params);

  void ovr_Shutdown();

  HmdDesc ovr_GetHmdDesc(Hmd hmd);

  int ovr_Create(PointerByReference hmd, PointerByReference luid);

  void ovr_Destroy(Hmd hmd);

  int ovr_GetSessionStatus(Hmd session, PointerByReference sessionStatus);

  Pointer ovr_GetVersionString();

  int ovr_RecenterTrackingOrigin(Hmd hmd);

  // String ovr_GetLastError(Hmd hmd);

  TrackingState ovr_GetTrackingState(Hmd hmd, double absTime, byte latencyMarker);

  OvrSizei ovr_GetFovTextureSize(Hmd hmd, int eye, FovPort fov, float pixelsPerDisplayPixel);

  EyeRenderDesc ovr_GetRenderDesc(Hmd hmd, int eyeType, FovPort fov);

  double ovr_GetPredictedDisplayTime(Hmd hmd, int frameIndex);

  double ovr_GetTimeInSeconds();

  byte ovr_GetBool(Hmd hmd, String propertyName, byte defaultVal);

  byte ovr_SetBool(Hmd hmd, String propertyName, byte value);

  int ovr_GetInt(Hmd hmd, String propertyName, int defaultVal);

  byte ovr_SetInt(Hmd hmd, String propertyName, int value);

  float ovr_GetFloat(Hmd hmd, String propertyName, float defaultVal);

  byte ovr_SetFloat(Hmd hmd, String propertyName, float value);

  int ovr_GetFloatArray(Hmd hmd, String propertyName, FloatBuffer values, int arraySize);

  byte ovr_SetFloatArray(Hmd hmd, String propertyName, FloatBuffer values, int arraySize);

  String ovr_GetString(Hmd hmd, String propertyName, String defaultVal);

  byte ovr_SetString(Hmd hmddesc, String propertyName, String value);

  OvrMatrix4f ovrMatrix4f_Projection(FovPort fov, float znear, float zfar, byte rightHanded);

  int ovr_CreateTextureSwapChainGL(Hmd session, TextureSwapChainDesc desc, PointerByReference out_TextureSwapChain);

  int ovr_GetTextureSwapChainBufferGL(Hmd session, Pointer chain, int index, IntByReference out_TexId);

  int ovr_GetTextureSwapChainLength(Hmd session, Pointer chain, IntByReference out_Length);

  int ovr_GetTextureSwapChainCurrentIndex(Hmd session, Pointer chain, IntByReference out_Index);

  int ovr_CommitTextureSwapChain(Hmd session, Pointer chain);

  void ovr_DestroyTextureSwapChain(Hmd session, Pointer chain);

  int ovr_CreateMirrorTextureGL(Hmd session, MirrorTextureDesc desc, PointerByReference out_MirrorTexture);

  int ovr_GetMirrorTextureBufferGL(Hmd session, Pointer mirrorTexture, IntByReference out_TexId);

  void ovr_DestroyMirrorTexture(Hmd session, Pointer mirrorTexture);

  int ovr_SubmitFrame(Hmd session, int frameIndex, Pointer viewScaleDesc, PointerByReference layers, int layerCount);

  int ovr_GetInputState(Hmd session, int inputState, InputState.ByReference ovrInputStat);

  public static class InputState extends Structure {
    public static class ByReference extends InputState implements Structure.ByReference {}
    
    public double TimeInSeconds;
    
    public int Buttons;
    
    public int Touches;
    
    public float IndexTrigger[] = new float[2];
    
    public float HandTrigger[] = new float[2];
    
    public OvrVector2f Thumbstick[] = new OvrVector2f[2];
    
    public int ControllerType;
    
    public float IndexTriggerNoDeadzone[] = new float[2];
    
    public float HandTriggerNoDeadzone[] = new float[2];
    
    public OvrVector2f ThumbstickNoDeadzone[] = new OvrVector2f[2];
    
    public float IndexTriggerRaw[] = new float[2];
    
    public float HandTriggerRaw[] = new float[2];
    
    public OvrVector2f ThumbstickRaw[] = new OvrVector2f[2];
    
    @Override
    protected List getFieldOrder() {
      // return Arrays.asList("Buttons", "ControllerType", "HandTrigger", "HandTriggerNoDeadzone", "HandTriggerRaw", "IndexTrigger", "IndexTriggerNoDeadzone", "IndexTriggerRaw", "Thumbstick", "ThumbstickNoDeadzone", "ThumbstickRaw", "TimeInSeconds", "Touches");
      return Arrays.asList("TimeInSeconds", "Buttons", "Touches", "IndexTrigger", "HandTrigger", "Thumbstick", "ControllerType", "IndexTriggerNoDeadzone", "HandTriggerNoDeadzone", "ThumbstickNoDeadzone", "IndexTriggerRaw", "HandTriggerRaw", "ThumbstickRaw");
      }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("TimeInSeconds:");
      sb.append(TimeInSeconds);
      sb.append(" ");
      sb.append("Buttons:");
      sb.append(Buttons);
      sb.append(" ");
      sb.append("Touches:");
      sb.append(Touches);
      sb.append(" ");
      sb.append("ControllerType:");
      sb.append(ControllerType);
      sb.append(" ");
      sb.append("HandTrigger:");
      sb.append(String.format("%.3f", HandTrigger[0]));
      sb.append(" ");
      sb.append(String.format("%.3f", HandTrigger[1]));
      sb.append(" ");
      sb.append("IndexTrigger:");
      sb.append(String.format("%.3f", IndexTrigger[0]));
      sb.append(" ");
      sb.append(String.format("%.3f", IndexTrigger[1]));
      sb.append(" ");
      sb.append("Thumbstick:");
      sb.append(String.format("%.3f", Thumbstick[0].x));
      sb.append(",");
      sb.append(String.format("%.3f", Thumbstick[0].y));
      sb.append(" ");
      sb.append(String.format("%.3f", Thumbstick[1].x));
      sb.append(",");
      sb.append(String.format("%.3f", Thumbstick[1].y));
      sb.append(" ");
      return sb.toString();
    }

  }

}
