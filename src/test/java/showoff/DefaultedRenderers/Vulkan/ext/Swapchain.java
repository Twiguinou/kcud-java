package showoff.DefaultedRenderers.Vulkan.ext;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import showoff.DefaultedRenderers.Vulkan.VulkanException;
import showoff.Disposable;
import showoff.FrameAllocator;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.KHRSurface.*;

public class Swapchain implements Disposable
{
    public record SurfaceFormat(int format, int color_space)
    {
        private SurfaceFormat(final VkSurfaceFormatKHR surfaceFormat)
        {
            this(surfaceFormat.format(), surfaceFormat.colorSpace());
        }
    }

    private long m_internalHandle;
    private final VkDevice device;
    private SurfaceFormat m_surfaceFormat;
    private long[] m_images;
    private long[] m_imageViews;
    private int m_width, m_height;

    public Swapchain(final VkDevice device)
    {
        this.m_internalHandle = VK_NULL_HANDLE;
        this.device = device;
        this.m_images = new long[0];
        this.m_imageViews = new long[0];
        this.m_surfaceFormat = null;
        this.m_width = 0;
        this.m_height = 0;
    }

    public static String getSwapchainExtensionName()
    {
        return VK_KHR_SWAPCHAIN_EXTENSION_NAME;
    }

    public static int getPresentSourceLayout()
    {
        return VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
    }

    private static SurfaceFormat selectSurfaceFormat(final VkSurfaceFormatKHR.Buffer surfaceFormats)
    {
        for (int i = 0; i < surfaceFormats.capacity(); i++)
        {
            final VkSurfaceFormatKHR c = surfaceFormats.get(i);
            if (c.format() == VK_FORMAT_B8G8R8A8_UNORM && c.colorSpace() == VK_COLORSPACE_SRGB_NONLINEAR_KHR)
            {
                return new SurfaceFormat(c);
            }
        }
        if (surfaceFormats.get(0).format() == VK_FORMAT_UNDEFINED)
        {
            return new SurfaceFormat(VK_FORMAT_B8G8R8A8_UNORM, VK_COLORSPACE_SRGB_NONLINEAR_KHR);
        }
        return new SurfaceFormat(surfaceFormats.get(0));
    }

    private static int selectPresentMode(int[] presentModes, boolean vsync)
    {
        final int wish = vsync ? VK_PRESENT_MODE_MAILBOX_KHR : VK_PRESENT_MODE_IMMEDIATE_KHR;
        int r = wish;
        for (int presentMode : presentModes)
        {
            if (presentMode == wish) return wish;
            if (presentMode == VK_PRESENT_MODE_MAILBOX_KHR || presentMode == VK_PRESENT_MODE_IMMEDIATE_KHR)
            {
                r = presentMode;
            }
        }
        return r;
    }

    private static VkExtent2D selectExtent(VkSurfaceCapabilitiesKHR capabilities, int width, int height)
    {
        if (capabilities.currentExtent().width() != 0xFFFFFFFF)
        {
            return capabilities.currentExtent();
        }
        return VkExtent2D.malloc(FrameAllocator.take())
                .width(Math.max(capabilities.minImageExtent().width(), Math.min(capabilities.maxImageExtent().width(), width)))
                .height(Math.max(capabilities.minImageExtent().height(), Math.min(capabilities.maxImageExtent().height(), height)));
    }

    public void initialize(VulkanRenderContext context, int graphics_queue_family, int present_queue_family, boolean vsync) throws VulkanException
    {
        this.m_images = new long[0];
        this.m_imageViews = new long[0];
        try (FrameAllocator allocator = FrameAllocator.takeAndPushIfEmpty())
        {
            final int imageCount = Math.max(context.getSurfaceProperties().capabilities().minImageCount() + 1, context.getSurfaceProperties().capabilities().maxImageCount());
            SurfaceFormat surfaceFormat = selectSurfaceFormat(context.getSurfaceProperties().formats());
            int presentMode = selectPresentMode(context.getSurfaceProperties().presentModes(), vsync);
            VkExtent2D extent = selectExtent(context.getSurfaceProperties().capabilities(), context.getWindowProcessor().getWidth(), context.getWindowProcessor().getHeight());
            VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(context.getSurface())
                    .minImageCount(imageCount)
                    .imageFormat(surfaceFormat.format)
                    .imageColorSpace(surfaceFormat.color_space)
                    .imageExtent(extent)
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT)
                    .preTransform(context.getSurfaceProperties().capabilities().currentTransform())
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(this.m_internalHandle);
            if (graphics_queue_family == present_queue_family)
            {
                swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }
            else
            {
                swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                        .queueFamilyIndexCount(2)
                        .pQueueFamilyIndices(allocator.ints(graphics_queue_family, present_queue_family));
            }
            LongBuffer pSwapchain = allocator.mallocLong(1);
            VulkanException.check(vkCreateSwapchainKHR(this.device, swapchainCreateInfo, null, pSwapchain), "Failed to create swapchain.");
            this.m_internalHandle = pSwapchain.get(0);
            this.m_surfaceFormat = surfaceFormat;
            this.m_width = extent.width();
            this.m_height = extent.height();

            IntBuffer pCount = allocator.mallocInt(1);
            VulkanException.check(vkGetSwapchainImagesKHR(this.device, this.m_internalHandle, pCount, null));
            LongBuffer pImages = allocator.mallocLong(pCount.get(0));
            VulkanException.check(vkGetSwapchainImagesKHR(this.device, this.m_internalHandle, pCount, pImages));
            this.m_images = new long[pCount.get(0)];
            pImages.get(this.m_images, 0, this.m_images.length);
        }
    }

    public long getHandle()
    {
        return this.m_internalHandle;
    }

    public SurfaceFormat getSurfaceFormat()
    {
        return this.m_surfaceFormat;
    }

    public long[] getImages()
    {
        return this.m_images;
    }

    public int getWidth()
    {
        return this.m_width;
    }

    public int getHeight()
    {
        return this.m_height;
    }

    public void createImageViews() throws VulkanException
    {
        if (this.m_images.length == 0) return;
        List<Long> im_views = new LinkedList<>();
        try (FrameAllocator allocator = FrameAllocator.takeAndPushIfEmpty())
        {
            VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .format(this.m_surfaceFormat.format)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D);
            imageViewCreateInfo.subresourceRange()
                    .layerCount(1)
                    .levelCount(1)
                    .baseMipLevel(0)
                    .baseArrayLayer(0)
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            LongBuffer pVkDest = allocator.mallocLong(1);
            for (int i = 0; i < this.m_images.length; i++)
            {
                imageViewCreateInfo.image(this.m_images[i]);
                VulkanException.check(vkCreateImageView(this.device, imageViewCreateInfo, null, pVkDest));
                im_views.add(pVkDest.get(0));
            }

            this.m_imageViews = im_views.stream().mapToLong(Long::longValue).toArray();
        }
        catch (VulkanException e)
        {
            im_views.forEach(i -> vkDestroyImageView(this.device, i, null));
            throw e;
        }
    }

    public long[] getImageViews()
    {
        return this.m_imageViews;
    }

    public int acquireNextImage(long semaphore, IntBuffer pImageIndex)
    {
        return vkAcquireNextImageKHR(this.device, this.m_internalHandle, -1L, semaphore, VK_NULL_HANDLE, pImageIndex);
    }

    @Override
    public void dispose()
    {
        for (long imageView : this.m_imageViews)
        {
            vkDestroyImageView(this.device, imageView, null);
        }
        this.m_images = new long[0];
        this.m_imageViews = new long[0];
        this.m_surfaceFormat = null;
        vkDestroySwapchainKHR(this.device, this.m_internalHandle, null);
        this.m_internalHandle = VK_NULL_HANDLE;
    }

}
