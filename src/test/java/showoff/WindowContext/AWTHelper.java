package showoff.WindowContext;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.jawt.*;
import org.lwjgl.system.windows.WinBase;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkWin32SurfaceCreateInfoKHR;
import showoff.FrameAllocator;

import java.awt.*;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRWin32Surface.*;

final class AWTHelper
{private AWTHelper() {}
    public static int VkSurface(VkInstance instance, VkAllocationCallbacks allocationCallbacks, Canvas canvas, LongBuffer pSurface)
    {
        return switch (Platform.get())
                {
                    case WINDOWS -> Win32VkSurface(instance, allocationCallbacks, canvas, pSurface);
                    default -> VK_ERROR_UNKNOWN;
                };
    }

    private static int Win32VkSurface(VkInstance instance, VkAllocationCallbacks allocator, Canvas canvas, LongBuffer pSurface)
    {
        try (FrameAllocator frameAllocator = FrameAllocator.takeAndPush())
        {
            JAWT njawt = JAWT.calloc(frameAllocator);
            njawt.version(JAWTFunctions.JAWT_VERSION_1_7);
            if (!JAWTFunctions.JAWT_GetAWT(njawt)) return VK_ERROR_UNKNOWN;
            JAWTDrawingSurface jSurface = JAWTFunctions.JAWT_GetDrawingSurface(canvas, njawt.GetDrawingSurface());
            if (jSurface == null) return VK_ERROR_UNKNOWN;
            if ((JAWTFunctions.JAWT_DrawingSurface_Lock(jSurface, jSurface.Lock()) & JAWTFunctions.JAWT_LOCK_ERROR) != 0)
            {
                JAWTFunctions.JAWT_FreeDrawingSurface(jSurface, njawt.FreeDrawingSurface());
                return VK_ERROR_UNKNOWN;
            }
            JAWTDrawingSurfaceInfo meta = JAWTFunctions.JAWT_DrawingSurface_GetDrawingSurfaceInfo(jSurface, jSurface.GetDrawingSurfaceInfo());
            if (meta == null)
            {
                JAWTFunctions.JAWT_DrawingSurface_Unlock(jSurface, jSurface.Unlock());
                JAWTFunctions.JAWT_FreeDrawingSurface(jSurface, njawt.FreeDrawingSurface());
                return VK_ERROR_UNKNOWN;
            }

            JAWTWin32DrawingSurfaceInfo win32_sinfo = JAWTWin32DrawingSurfaceInfo.create(meta.platformInfo());
            long handle = WinBase.nGetModuleHandle(MemoryUtil.NULL);
            VkWin32SurfaceCreateInfoKHR surfaceCreateInfo = VkWin32SurfaceCreateInfoKHR.calloc(frameAllocator)
                    .sType(VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR)
                    .hinstance(handle)
                    .hwnd(win32_sinfo.hwnd());
            final int result = vkCreateWin32SurfaceKHR(instance, surfaceCreateInfo, allocator, pSurface);

            JAWTFunctions.JAWT_DrawingSurface_FreeDrawingSurfaceInfo(meta, jSurface.FreeDrawingSurfaceInfo());
            JAWTFunctions.JAWT_DrawingSurface_Unlock(jSurface, jSurface.Unlock());
            JAWTFunctions.JAWT_FreeDrawingSurface(jSurface, njawt.FreeDrawingSurface());

            return result;
        }
    }

    public static String getSurfaceExtension()
    {
        return switch (Platform.get())
                {
                    case WINDOWS -> VK_KHR_WIN32_SURFACE_EXTENSION_NAME;
                    default -> null;
                };
    }
}
