package showoff.App;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.*;
import showoff.DefaultedRenderers.Vulkan.CommandPool;
import showoff.DefaultedRenderers.Vulkan.LogicalDevice;
import showoff.DefaultedRenderers.Vulkan.VulkanException;
import showoff.DefaultedRenderers.Vulkan.VulkanHelpers;
import showoff.DefaultedRenderers.Vulkan.ext.Swapchain;
import showoff.DefaultedRenderers.Vulkan.ext.VulkanRenderContext;
import showoff.FrameAllocator;
import showoff.WindowContext.GLFWWindowProcessor;
import showoff.WindowContext.WindowProcessor;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.KHRTimelineSemaphore.*;

public class VulkanRenderer
{
    private static final Logger gRendererLogger = LogManager.getLogger("Vulkan Renderer");
    private static final int gMaxFrameCount = 2;

    private final VulkanRenderContext m_context;
    private final WindowProcessor m_windowProc = new GLFWWindowProcessor();
    private boolean m_running = false;
    private final LogicalDevice m_logicalDevice;
    private final LogicalDevice.Queue m_graphicsQueue;
    private final Swapchain m_swapchain;

    private final CommandPool m_commandPool;

    public VulkanRenderer(String name)
    {
        this.m_windowProc.setWindowTitle(name);
        this.m_windowProc.createWindowContext(1280, 960);
        this.m_windowProc.setWndKeyInputCallback((key, scancode, action, mods) ->
        {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS)
            {
                this.m_running = false;
            }
        });

        this.m_context = new VulkanRenderContext(name, 0, "kcud", 0, VK_API_VERSION_1_3, this.m_windowProc.getVulkanExtensions(), VulkanRenderContext::DefaultCallbackFunction, this.m_windowProc);
        this.m_context.findSuitableDevice((first, second) ->
        {
            int i = 0;
            if (first.isDedicated()) ++i;
            if (second.isDedicated()) --i;
            if (first.features().samplerAnisotropy()) ++i;
            if (second.features().samplerAnisotropy()) --i;
            return i > 0 ? first : second;
        });
        gRendererLogger.info("Selected physical device: " + this.m_context.getPhysicalDevice().properties().deviceNameString());

        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            LogicalDevice.QueueRequirements[] queueRequirements = new LogicalDevice.QueueRequirements[]
                    {
                            new LogicalDevice.QueueRequirements(f -> (f & VK_QUEUE_GRAPHICS_BIT) != 0, new long[]{this.m_context.getSurface()}, null, false)
                    };

            VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .features(f -> f
                            .samplerAnisotropy(true)
                            .sampleRateShading(true));
            VkPhysicalDeviceVulkan12Features features = VkPhysicalDeviceVulkan12Features.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
                    .pNext(features2.address())
                    .timelineSemaphore(true);

            this.m_logicalDevice = new LogicalDevice(this.m_context.getPhysicalDevice(), queueRequirements,
                    new String[]
                    {
                            KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME,
                            VK_KHR_TIMELINE_SEMAPHORE_EXTENSION_NAME
                    }, features.address(), null, true);
        }
        this.m_graphicsQueue = this.m_logicalDevice.generatedQueues.get(0);

        this.m_swapchain = new Swapchain(this.m_logicalDevice.get());
        this.m_swapchain.initialize(this.m_context, this.m_graphicsQueue.family(), this.m_graphicsQueue.family(), true);

        this.m_commandPool = new CommandPool(this.m_logicalDevice.get(), 0, this.m_graphicsQueue);
    }

    public void run()
    {
        this.m_running = true;
        final long renderPass;
        final long[] framebuffers;
        final VkCommandBuffer[] commandsBuffers;
        final long renderFence;
        final long renderSemaphore, imageSemaphore;
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, allocator).apply(0, desc -> desc
                    .format(this.m_swapchain.getSurfaceFormat().format())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR));

            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.calloc(1, allocator).apply(0, subpass -> subpass
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1)
                    .pColorAttachments(VkAttachmentReference.calloc(1, allocator).apply(0, ref -> ref
                            .attachment(0)
                            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL))));

            VkRenderPassCreateInfo renderPassCreateInfo = VkRenderPassCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(attachments)
                    .pSubpasses(subpasses);
            LongBuffer pVkDest = allocator.mallocLong(1);
            VulkanException.check(vkCreateRenderPass(this.m_logicalDevice.get(), renderPassCreateInfo, null, pVkDest));
            renderPass = pVkDest.get(0);

            VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(renderPass)
                    .attachmentCount(1)
                    .pAttachments(allocator.mallocLong(1))
                    .width(this.m_swapchain.getWidth())
                    .height(this.m_swapchain.getHeight())
                    .layers(1);
            framebuffers = Arrays.stream(this.m_swapchain.getImages()).mapToLong(image ->
            {
                framebufferCreateInfo.pAttachments().put(0, image.imageView());
                VulkanException.check(vkCreateFramebuffer(this.m_logicalDevice.get(), framebufferCreateInfo, null, pVkDest));
                return pVkDest.get(0);
            }).toArray();

            renderFence = VulkanHelpers.createFence(allocator, this.m_logicalDevice.get(), VK_FENCE_CREATE_SIGNALED_BIT);
            renderSemaphore = VulkanHelpers.createSemaphore(allocator, this.m_logicalDevice.get(), VK_SEMAPHORE_TYPE_BINARY, 0);
            imageSemaphore = VulkanHelpers.createSemaphore(allocator, this.m_logicalDevice.get(), VK_SEMAPHORE_TYPE_BINARY, 0);

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .renderArea(r -> r.extent(e -> e.set(this.m_swapchain.getWidth(), this.m_swapchain.getHeight())))
                    .clearValueCount(1)
                    .pClearValues(VkClearValue.calloc(1, allocator).apply(0, v ->
                            v.color().float32()
                                    .put(0, .3f)
                                    .put(1, .5f)
                                    .put(2, .7f)
                                    .put(3, 1.f)));
            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            commandsBuffers = this.m_commandPool.allocate(framebuffers.length, VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            for (int i = 0; i < framebuffers.length; i++)
            {
                VulkanException.check(vkBeginCommandBuffer(commandsBuffers[i], commandBufferBeginInfo));
                vkCmdBeginRenderPass(commandsBuffers[i], renderPassBeginInfo.framebuffer(framebuffers[i]), VK_SUBPASS_CONTENTS_INLINE);
                vkCmdEndRenderPass(commandsBuffers[i]);
                VulkanException.check(vkEndCommandBuffer(commandsBuffers[i]));
            }
        }

        while (this.m_running && !this.m_windowProc.windowShouldClose())
        {
            this.m_windowProc.beginRenderStage();

            try (FrameAllocator allocator = FrameAllocator.takeAndPush())
            {
                IntBuffer pImageIndex = allocator.mallocInt(1);
                VulkanException.check(this.m_swapchain.acquireNextImage(imageSemaphore, pImageIndex));

                VulkanException.check(vkWaitForFences(this.m_logicalDevice.get(), renderFence, true, Long.MAX_VALUE));
                VulkanException.check(vkResetFences(this.m_logicalDevice.get(), renderFence));
                VkSubmitInfo submitInfo = VkSubmitInfo.calloc(allocator)
                        .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                        .waitSemaphoreCount(1)
                        .pWaitSemaphores(allocator.longs(imageSemaphore))
                        .pWaitDstStageMask(allocator.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                        .pCommandBuffers(allocator.pointers(commandsBuffers[pImageIndex.get(0)]))
                        .pSignalSemaphores(allocator.longs(renderSemaphore));
                VulkanException.check(vkQueueSubmit(this.m_graphicsQueue.handle(), submitInfo, renderFence));

                VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(allocator)
                        .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                        .pWaitSemaphores(allocator.longs(renderSemaphore))
                        .swapchainCount(1)
                        .pSwapchains(allocator.longs(this.m_swapchain.get()))
                        .pImageIndices(pImageIndex);
                VulkanException.check(KHRSwapchain.vkQueuePresentKHR(this.m_graphicsQueue.handle(), presentInfo));
            }

            this.m_windowProc.endRenderStage();
        }

        VulkanException.check(vkDeviceWaitIdle(this.m_logicalDevice.get()));
        this.m_commandPool.free(commandsBuffers);
        vkDestroyFence(this.m_logicalDevice.get(), renderFence, null);
        vkDestroySemaphore(this.m_logicalDevice.get(), renderSemaphore, null);
        vkDestroySemaphore(this.m_logicalDevice.get(), imageSemaphore, null);
        for (long framebuffer : framebuffers) vkDestroyFramebuffer(this.m_logicalDevice.get(), framebuffer, null);
        vkDestroyRenderPass(this.m_logicalDevice.get(), renderPass, null);
        this.destroy();
    }

    public void destroy()
    {
        this.m_commandPool.dispose();
        this.m_swapchain.dispose();
        this.m_logicalDevice.dispose();
        this.m_context.dispose();
        this.m_windowProc.dispose();
    }
}
