package showoff.DefaultedRenderers.Vulkan.ext;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import showoff.DefaultedRenderers.Vulkan.LogicalDevice;
import showoff.ForeignStack;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.KHRSurface.*;

public class Swapchain
{
    private long m_internalHandle;
    private final VkDevice device;
    private VkSwapchainCreateInfoKHR m_swapchainCreateInfo;

    public Swapchain(final VkDevice device)
    {
        this.m_internalHandle = VK_NULL_HANDLE;
        this.device = device;
        this.m_swapchainCreateInfo = null;
    }

    public static VkSurfaceFormatKHR selectSurfaceFormat(final VkSurfaceFormatKHR.Buffer surfaceFormats)
    {
        assert surfaceFormats.capacity() != 0;
        for (int i = 0; i < surfaceFormats.capacity(); i++)
        {
            final VkSurfaceFormatKHR c = surfaceFormats.get(i);
            if (c.format() == VK_FORMAT_B8G8R8A8_UNORM && c.colorSpace() == VK_COLORSPACE_SRGB_NONLINEAR_KHR)
            {
                return c;
            }
        }
        return surfaceFormats.get(0);
    }

    public int initialize(int minImageCount, final SurfaceHandle surface, VkSurfaceFormatKHR format, LogicalDevice.QueueProperties graphicsQueue, LogicalDevice.QueueProperties presentQueue)
    {
        if (this.m_internalHandle != VK_NULL_HANDLE) this.dispose();

        try (ForeignStack stack = ForeignStack.pushConfined(256))
        {
            VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
            SurfaceHandle.capabilities(surface.handle(), this.device.getPhysicalDevice(), capabilities);

            VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc();
            swapchainCreateInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            swapchainCreateInfo.pNext(MemoryUtil.NULL);
            swapchainCreateInfo.flags(0);
            swapchainCreateInfo.surface(surface.handle());
            swapchainCreateInfo.minImageCount(minImageCount);
            swapchainCreateInfo.imageFormat(format.format());
            swapchainCreateInfo.imageColorSpace(format.colorSpace());
            swapchainCreateInfo.imageExtent(capabilities.currentExtent());
            swapchainCreateInfo.imageArrayLayers(1);
            swapchainCreateInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_STORAGE_BIT);
            swapchainCreateInfo.preTransform(capabilities.currentTransform());
            swapchainCreateInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            swapchainCreateInfo.presentMode(VK_PRESENT_MODE_FIFO_KHR);
            swapchainCreateInfo.clipped(false);
            swapchainCreateInfo.oldSwapchain(MemoryUtil.NULL);

            final IntBuffer pIdxs;
            if (graphicsQueue.family() == presentQueue.family())
            {
                swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                swapchainCreateInfo.queueFamilyIndexCount(2);
                pIdxs = stack.ints(graphicsQueue.family(), presentQueue.family());
            }
            else
            {
                swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
                swapchainCreateInfo.queueFamilyIndexCount(0);
                pIdxs = null;
            }
            swapchainCreateInfo.pQueueFamilyIndices(pIdxs);

            LongBuffer pSwapchain = stack.mallocLong(1);
            int vk_result = vkCreateSwapchainKHR(this.device, swapchainCreateInfo, null, pSwapchain);
            if (vk_result != VK_SUCCESS)
            {
                System.err.println("Failed to create vulkan swapchain.");
                return vk_result;
            }
            this.m_internalHandle = pSwapchain.get(0);
            this.m_swapchainCreateInfo = swapchainCreateInfo;

            return VK_SUCCESS;
        }
    }

    public int presentImage(VkQueue queue, long waitSemaphore, int imageIndex)
    {
        try (ForeignStack stack = ForeignStack.pushConfined(128))
        {
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pNext(MemoryUtil.NULL);
            presentInfo.pWaitSemaphores(stack.longs(waitSemaphore));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(this.m_internalHandle));
            presentInfo.pImageIndices(stack.ints(imageIndex));
            return vkQueuePresentKHR(queue, presentInfo);
        }
    }

    public void dispose()
    {
        if (this.m_internalHandle == VK_NULL_HANDLE) throw new IllegalStateException("Swapchain handle is null.");
        vkDestroySwapchainKHR(this.device, this.m_internalHandle, null);
        this.m_swapchainCreateInfo.free();
        this.m_swapchainCreateInfo = null;
        this.m_internalHandle = VK_NULL_HANDLE;
    }

    public long getHandle() {return this.m_internalHandle;}

}
